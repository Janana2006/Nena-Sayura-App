package lk.nenasayura.app.ui.student.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.Calendar
import lk.nenasayura.app.data.model.StudentProfile

private val SriLankaDistricts = listOf(
    "Ampara", "Anuradhapura", "Badulla", "Batticaloa", "Colombo", "Galle", "Gampaha",
    "Hambantota", "Jaffna", "Kalutara", "Kandy", "Kegalle", "Kilinochchi", "Kurunegala",
    "Mannar", "Matale", "Matara", "Monaragala", "Mullaitivu", "Nuwara Eliya",
    "Polonnaruwa", "Puttalam", "Ratnapura", "Trincomalee", "Vavuniya"
)

/** Edit name, phone, school, district and A/L exam year. */
@Composable
fun PersonalDetailsScreen(
    profile: StudentProfile,
    saving: Boolean,
    onBack: () -> Unit,
    onSave: (name: String, phone: String, school: String, district: String, examYear: Int) -> Unit
) {
    var name by rememberSaveable(profile.name) { mutableStateOf(profile.name) }
    var phone by rememberSaveable(profile.phone) { mutableStateOf(profile.phone) }
    var school by rememberSaveable(profile.school) { mutableStateOf(profile.school) }
    var district by rememberSaveable(profile.district) { mutableStateOf(profile.district) }
    var examYear by rememberSaveable(profile.examYear) { mutableIntStateOf(profile.examYear) }

    val thisYear = Calendar.getInstance().get(Calendar.YEAR)
    val yearOptions = (thisYear..thisYear + 3).map { it.toString() }

    val changed = name.trim() != profile.name ||
            phone.trim() != profile.phone ||
            school.trim() != profile.school ||
            district != profile.district ||
            examYear != profile.examYear

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SubPageHeader(
            title = "Personal details",
            subtitle = "Keep your details up to date",
            onBack = onBack
        )

        FormCard {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Full name") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(modifier = Modifier.size(10.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Phone (optional)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(modifier = Modifier.size(10.dp))
            OutlinedTextField(
                value = school,
                onValueChange = { school = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("School (optional)") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(modifier = Modifier.size(10.dp))
            PickerField(
                label = "District",
                value = district,
                options = SriLankaDistricts,
                onSelect = { district = it }
            )
            Spacer(modifier = Modifier.size(10.dp))
            PickerField(
                label = "A/L exam year",
                value = if (examYear > 0) examYear.toString() else "",
                options = yearOptions,
                onSelect = { examYear = it.toInt() }
            )
            Spacer(modifier = Modifier.size(16.dp))
            PrimaryActionButton(
                text = "Save changes",
                busy = saving,
                enabled = changed && name.isNotBlank(),
                onClick = { onSave(name.trim(), phone.trim(), school.trim(), district, examYear) }
            )
        }
    }
}