package lk.nenasayura.app.ui.admin

import com.google.firebase.auth.FirebaseAuth
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.nenasayura.app.data.model.AdminPermission
import lk.nenasayura.app.data.model.AppUser
import lk.nenasayura.app.data.model.GuideBook
import lk.nenasayura.app.data.model.Paper
import lk.nenasayura.app.data.model.Payment
import lk.nenasayura.app.data.model.StaffRole
import lk.nenasayura.app.data.model.StudyStream
import lk.nenasayura.app.data.model.Subject
import lk.nenasayura.app.data.model.ContentStatus
import lk.nenasayura.app.data.model.TeacherContent
import lk.nenasayura.app.data.repository.AdminRepository
import lk.nenasayura.app.data.model.Payout
import lk.nenasayura.app.data.repository.PayoutRepository
import lk.nenasayura.app.data.repository.TeacherContentRepository
import lk.nenasayura.app.util.round2

/** What one teacher earned: students who paid, total paid, admin's cut and the rest. */
data class TeacherSummary(
    val teacherId: String,
    val teacherName: String,
    val students: Int,
    val gross: Double,
    val commission: Double,
    val bankName: String = "",
    val accountNumber: String = "",
    val bankBranch: String = "",
    /** What the admin has already sent to this teacher (sum of payouts). */
    val paidOut: Double = 0.0
) {
    val net: Double get() = gross - commission

    /** Still to pay to this teacher. Becomes 0 once everything is paid. */
    val due: Double get() = maxOf(0.0, round2(net - paidOut))
    val hasBankDetails: Boolean
        get() = bankName.isNotBlank() && accountNumber.isNotBlank() && bankBranch.isNotBlank()
}

data class AdminUiState(
    val checking: Boolean = true,
    val accessDenied: Boolean = false,
    /** What the logged-in admin may open. Empty until the role check finishes. */
    val access: Set<AdminPermission> = emptySet(),
    val loading: Boolean = true,
    val users: List<AppUser> = emptyList(),
    val payments: List<Payment> = emptyList(),
    val payouts: List<Payout> = emptyList(),
    val commissionPercent: Double = 10.0,
    val papers: List<Paper> = emptyList(),
    val guideBooks: List<GuideBook> = emptyList(),
    val streams: List<StudyStream> = emptyList(),
    /** Every teacher upload, for the admin to look at and remove. */
    val teacherContents: List<TeacherContent> = emptyList(),
    /** True while something is being saved / uploaded. */
    val busy: Boolean = false,
    /** One-time message shown in a snackbar. */
    val message: String? = null
) {
    fun can(permission: AdminPermission): Boolean = permission in access
    val canSeeContent: Boolean get() = access.any { it in AdminPermission.CONTENT }

    /** Teacher uploads waiting for this admin's decision (from students who became teachers). */
    val pendingCount: Int get() = teacherContents.count { it.status == ContentStatus.PENDING }

    val students: List<AppUser> get() = users.filter { it.isStudent }
    val teachers: List<AppUser> get() = users.filter { it.isTeacher }
    val staff: List<AppUser> get() = users.filter { it.isAdmin }

    val paidPayments: List<Payment> get() = payments.filter { it.isPaid }
    val gross: Double get() = paidPayments.sumOf { it.amount }

    // Each payment uses the % saved when it was made (or the current default)
    val commission: Double
        get() = paidPayments.sumOf { it.amount * (it.commissionPercent ?: commissionPercent) / 100.0 }
    val teachersTotal: Double get() = gross - commission

    /** Everything already paid out to teachers. */
    val payoutsTotal: Double get() = round2(payouts.sumOf { it.amount })

    /** Everything still to pay to teachers. */
    val stillToPay: Double get() = round2(teacherSummaries.sumOf { it.due })

    val teacherSummaries: List<TeacherSummary>
        get() = paidPayments.groupBy { it.teacherId }.map { (id, list) ->
            val teacher = users.firstOrNull { it.uid == id }
            TeacherSummary(
                teacherId = id,
                teacherName = list.firstOrNull { it.teacherName.isNotBlank() }?.teacherName ?: "Unknown teacher",
                students = list.map { it.studentId }.distinct().size,
                gross = list.sumOf { it.amount },
                commission = list.sumOf { it.amount * (it.commissionPercent ?: commissionPercent) / 100.0 },
                bankName = teacher?.bankName.orEmpty(),
                accountNumber = teacher?.accountNumber.orEmpty(),
                bankBranch = teacher?.bankBranch.orEmpty(),
                paidOut = payouts.filter { it.teacherId == id }.sumOf { it.amount }
            )
        }.sortedByDescending { it.gross }

    /** Every subject of every stream, once each (papers and books only store the subject id). */
    val allSubjects: List<Subject>
        get() = streams.flatMap { it.subjects }.distinctBy { it.id }.sortedBy { it.name }

    fun subjectName(id: String): String = allSubjects.firstOrNull { it.id == id }?.name ?: id
}

class AdminViewModel(
    private val repo: AdminRepository = AdminRepository(),
    private val contentRepo: TeacherContentRepository = TeacherContentRepository(),
    private val payoutRepo: PayoutRepository = PayoutRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(AdminUiState())
    val state: StateFlow<AdminUiState> = _state.asStateFlow()

    private var startedFor: String? = null
    private val loadErrors = mutableListOf<String>()

    /** Called once when the Admin Home opens: checks the role + access, then loads everything. */
    fun start(uid: String) {
        if (startedFor == uid) return
        startedFor = uid
        viewModelScope.launch {
            val access = try {
                repo.getAdminAccess(uid)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Role check failed: ${e.localizedMessage}")
                null
            }
            if (access == null) {
                _state.update { it.copy(checking = false, accessDenied = true, loading = false) }
                return@launch
            }
            _state.update { it.copy(checking = false, access = access) }
            loadAll()
        }
    }

    fun reload() {
        viewModelScope.launch { loadAll() }
    }

    // One section failing (for example a rules problem) never blanks the whole dashboard.
    private suspend fun loadAll() {
        _state.update { it.copy(loading = true) }
        loadErrors.clear()

        val users = safe("users", emptyList<AppUser>()) { repo.getUsers() }
        val payments = safe("payments", emptyList<Payment>()) { repo.getPayments() }
        val payouts = safe("payouts", emptyList<Payout>()) { payoutRepo.getAllPayouts() }
        val percent = safe("commission", 10.0) { repo.getCommissionPercent() }
        val papers = safe("papers", emptyList<Paper>()) { repo.getPapers() }
        val books = safe("guide books", emptyList<GuideBook>()) { repo.getGuideBooks() }
        val streams = safe("streams", emptyList<StudyStream>()) { repo.getStreams() }
        val teacherContents = safe("teacher uploads", emptyList<TeacherContent>()) {
            if (_state.value.can(AdminPermission.TEACHER_UPLOADS)) contentRepo.getAllForReview() else emptyList()
        }

        _state.update {
            it.copy(
                loading = false,
                users = users,
                payments = payments,
                payouts = payouts,
                commissionPercent = percent,
                papers = papers,
                guideBooks = books,
                streams = streams,
                teacherContents = teacherContents,
                message = if (loadErrors.isEmpty()) it.message
                else "Could not load: ${loadErrors.joinToString()}. Check the Firestore rules."
            )
        }
    }

    private suspend fun <T> safe(label: String, fallback: T, block: suspend () -> T): T =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "$label failed: ${e.localizedMessage}")
            loadErrors.add(label)
            fallback
        }

    // ---------------- papers ----------------

    /** [onResult] gets null on success, or an error message to show in the dialog. */
    fun addPaper(
        title: String,
        subject: Subject?,
        yearText: String,
        type: String,
        term: Int,
        province: String,
        file: PickedFile?,
        link: String,
        onResult: (String?) -> Unit
    ) {
        val year = yearText.trim().toIntOrNull()
        val error = when {
            title.isBlank() -> "Enter a title."
            subject == null -> "Choose a subject."
            year == null || year < 1990 || year > 2100 -> "Enter a valid year (for example 2024)."
            type == "provincial" && province.isBlank() -> "Choose a province."
            type == "provincial" && term !in 1..3 -> "Choose a term (1st, 2nd or 3rd)."
            file == null && !isLink(link) -> "Choose a file, or paste a link that starts with https://"
            else -> null
        }
        if (error != null || subject == null || year == null) {
            onResult(error)
            return
        }
        act("Paper added", onResult) {
            val url = if (file != null) repo.uploadFile("papers/$type", file.uri, file.name) else link.trim()
            repo.addPaper(title, subject.id, year, type, url, term, province)
            val papers = repo.getPapers()
            _state.update { it.copy(papers = papers) }
        }
    }

    fun deletePaper(paper: Paper) {
        act("Paper deleted", this::toast) {
            repo.deletePaper(paper.id)
            _state.update { s -> s.copy(papers = s.papers.filterNot { it.id == paper.id }) }
        }
    }

    // ---------------- guide books ----------------

    fun addGuideBook(
        title: String,
        subject: Subject?,
        author: String,
        category: String,
        file: PickedFile?,
        link: String,
        onResult: (String?) -> Unit
    ) {
        val error = when {
            title.isBlank() -> "Enter a title."
            subject == null -> "Choose a subject."
            file == null && !isLink(link) -> "Choose a file, or paste a link that starts with https://"
            else -> null
        }
        if (error != null || subject == null) {
            onResult(error)
            return
        }
        act("Guide book added", onResult) {
            val url = if (file != null) repo.uploadFile("guide_books/$category", file.uri, file.name) else link.trim()
            repo.addGuideBook(title, subject.id, author, category, url)
            val books = repo.getGuideBooks()
            _state.update { it.copy(guideBooks = books) }
        }
    }

    fun deleteGuideBook(book: GuideBook) {
        act("Guide book deleted", this::toast) {
            repo.deleteGuideBook(book.id)
            _state.update { s -> s.copy(guideBooks = s.guideBooks.filterNot { it.id == book.id }) }
        }
    }

    // ---------------- streams, subjects and groups ----------------

    fun addStream(name: String, onResult: (String?) -> Unit) {
        if (name.isBlank()) return onResult("Enter the stream name.")
        act("Stream added", onResult) {
            repo.addStream(name)
            refreshStreams()
        }
    }

    fun deleteStream(stream: StudyStream) {
        act("Stream deleted", this::toast) {
            repo.deleteStream(stream.id)
            refreshStreams()
        }
    }

    fun addSubject(streamId: String, name: String, kanda: Int, onResult: (String?) -> Unit) {
        if (name.isBlank()) return onResult("Enter the subject name.")
        act("Subject added to Group $kanda", onResult) {
            repo.addSubject(streamId, name, kanda)
            refreshStreams()
        }
    }

    fun moveSubject(streamId: String, subject: Subject, kanda: Int, onResult: (String?) -> Unit) {
        act("Moved to Group $kanda", onResult) {
            repo.moveSubject(streamId, subject.id, kanda)
            refreshStreams()
        }
    }

    fun deleteSubject(streamId: String, subject: Subject) {
        act("Subject removed", this::toast) {
            repo.deleteSubject(streamId, subject.id)
            refreshStreams()
        }
    }

    private suspend fun refreshStreams() {
        val streams = repo.getStreams()
        _state.update { it.copy(streams = streams) }
    }

    // ---------------- staff ----------------

    fun addStaff(
        name: String,
        email: String,
        password: String,
        role: StaffRole,
        permissions: Set<AdminPermission>,
        onResult: (String?) -> Unit
    ) {
        val error = when {
            !_state.value.can(AdminPermission.STAFF) -> "You do not have access to add staff."
            name.isBlank() -> "Enter the name."
            !email.contains("@") || !email.contains(".") -> "Enter a valid email."
            password.length < 6 -> "The password must be at least 6 characters."
            permissions.isEmpty() -> "Tick at least one access."
            else -> null
        }
        if (error != null) {
            onResult(error)
            return
        }
        // A staff member can only hand out access they have themselves
        val allowed = permissions.intersect(_state.value.access)
        act("Staff member added", onResult) {
            repo.createStaff(name, email, password, role.key, allowed)
            val users = repo.getUsers()
            _state.update { it.copy(users = users) }
        }
    }

    fun updateStaffAccess(
        member: AppUser,
        role: StaffRole,
        permissions: Set<AdminPermission>,
        onResult: (String?) -> Unit
    ) {
        if (!_state.value.can(AdminPermission.STAFF)) return onResult("You do not have access to edit staff.")
        if (permissions.isEmpty()) return onResult("Tick at least one access.")
        val allowed = permissions.intersect(_state.value.access)
        act("Access updated", onResult) {
            repo.updateStaffAccess(member.uid, role.key, allowed)
            val users = repo.getUsers()
            _state.update { it.copy(users = users) }
        }
    }

    // ---------------- commission ----------------

    fun setCommission(text: String, onResult: (String?) -> Unit) {
        val percent = text.trim().toDoubleOrNull()
        if (percent == null || percent < 0.0 || percent > 100.0) {
            return onResult("Enter a number between 0 and 100.")
        }
        act("Commission updated", onResult) {
            repo.setCommissionPercent(percent)
            _state.update { it.copy(commissionPercent = percent) }
        }
    }

    // ---------------- paying teachers ----------------

    /**
     * Records a payment to a teacher: uploads the receipt, saves the payout and notifies the teacher.
     * [onResult] gets null on success, or an error message to show on the form.
     */
    fun payTeacher(
        teacherId: String,
        amountText: String,
        reference: String,
        receipt: PickedFile?,
        onResult: (String?) -> Unit
    ) {
        val summary = _state.value.teacherSummaries.firstOrNull { it.teacherId == teacherId }
        val amount = amountText.trim().toDoubleOrNull()?.let { round2(it) }
        val error = when {
            summary == null -> "This teacher could not be found."
            !summary.hasBankDetails -> "The teacher has not added bank details yet."
            amount == null || amount <= 0.0 -> "Enter the amount you paid."
            amount > summary.due + 0.005 -> "The amount is more than the ${money(summary.due)} still to pay."
            receipt == null -> "Add the payment receipt (photo or PDF)."
            else -> null
        }
        if (error != null || summary == null || amount == null || receipt == null) {
            onResult(error)
            return
        }

        val admin = FirebaseAuth.getInstance().currentUser
        val adminName = _state.value.users.firstOrNull { it.uid == admin?.uid }?.name
            ?.takeIf { it.isNotBlank() } ?: admin?.email.orEmpty()

        act("Payment recorded. The teacher was notified.", onResult) {
            payoutRepo.createPayout(
                teacherId = summary.teacherId,
                teacherName = summary.teacherName,
                amount = amount,
                reference = reference.trim(),
                receiptUri = receipt.uri,
                receiptName = receipt.name,
                adminUid = admin?.uid.orEmpty(),
                adminName = adminName
            )
            val payouts = payoutRepo.getAllPayouts()
            _state.update { it.copy(payouts = payouts) }
        }
    }

    // ---------------- teacher uploads (approve / decline / remove) ----------------

    /** Makes the upload visible to students and clears any old decline message. */
    fun approveContent(content: TeacherContent) {
        val adminUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        act("Approved \"${content.title}\"", this::toast) {
            contentRepo.approve(content.id, adminUid)
            _state.update { s ->
                s.copy(
                    teacherContents = s.teacherContents.map {
                        if (it.id == content.id) {
                            it.copy(
                                status = ContentStatus.APPROVED,
                                declineReason = "",
                                reviewedAtMillis = System.currentTimeMillis()
                            )
                        } else it
                    }
                )
            }
        }
    }

    /** Declines the upload. The reason is saved so the teacher can read it. */
    fun declineContent(content: TeacherContent, reason: String, onResult: (String?) -> Unit) {
        val text = reason.trim()
        if (text.length < 5) {
            onResult("Please write a short reason so the teacher knows what to fix.")
            return
        }
        val adminUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        act("Declined \"${content.title}\"", onResult) {
            contentRepo.decline(content.id, text, adminUid)
            _state.update { s ->
                s.copy(
                    teacherContents = s.teacherContents.map {
                        if (it.id == content.id) {
                            it.copy(
                                status = ContentStatus.DECLINED,
                                declineReason = text,
                                reviewedAtMillis = System.currentTimeMillis()
                            )
                        } else it
                    }
                )
            }
        }
    }

    /** Removes a teacher's upload for good. Students and the teacher no longer see it. */
    fun removeContent(content: TeacherContent) {
        act("Removed \"${content.title}\"", this::toast) {
            contentRepo.delete(content)
            _state.update { s ->
                s.copy(teacherContents = s.teacherContents.filter { it.id != content.id })
            }
        }
    }

    // ---------------- helpers ----------------

    fun consumeMessage() = _state.update { it.copy(message = null) }

    private fun toast(error: String?) {
        if (error != null) _state.update { it.copy(message = error) }
    }

    private fun isLink(url: String): Boolean {
        val u = url.trim()
        return u.startsWith("https://") || u.startsWith("http://")
    }

    /** Runs [block] with the busy flag on; reports success or the error through [onResult]. */
    private fun act(success: String, onResult: (String?) -> Unit, block: suspend () -> Unit) {
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                block()
                _state.update { it.copy(busy = false, message = success) }
                onResult(null)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(busy = false) }
                onResult(e.localizedMessage ?: "Something went wrong.")
            }
        }
    }

    private companion object {
        const val TAG = "AdminVM"
    }
}