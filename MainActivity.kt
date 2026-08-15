package nl.ton.meditatiehealth

import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.MindfulnessSessionRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.time.Instant

class MainActivity : AppCompatActivity() {
    private lateinit var healthConnectClient: HealthConnectClient
    private var selectedMinutes = 20

    private val permission = HealthPermission.getWritePermission(MindfulnessSessionRecord::class)
    private val requestPermission = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        if (permission in granted) {
            Toast.makeText(this, "Toestemming gegeven", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        healthConnectClient = HealthConnectClient.getOrCreate(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 50, 40, 40)
        }
        val title = TextView(this).apply {
            text = "Meditatie registreren"
            textSize = 26f
        }
        root.addView(title)

        val info = TextView(this).apply {
            text = "Kies de duur. De sessie eindigt op het moment dat je op Opslaan tikt."
            textSize = 16f
            setPadding(0, 20, 0, 20)
        }
        root.addView(info)

        val group = RadioGroup(this).apply { orientation = RadioGroup.VERTICAL }
        listOf(5, 10, 15, 20, 30).forEach { min ->
            val rb = RadioButton(this).apply {
                text = "$min minuten"
                isChecked = min == 20
                setOnClickListener { selectedMinutes = min }
            }
            group.addView(rb)
        }
        root.addView(group)

        val custom = EditText(this).apply {
            hint = "Andere duur in minuten"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        root.addView(custom)

        val save = Button(this).apply {
            text = "Opslaan in Health Connect"
            setOnClickListener {
                val customVal = custom.text.toString().toIntOrNull()
                val minutes = customVal?.takeIf { it in 1..720 } ?: selectedMinutes
                saveMeditation(minutes)
            }
        }
        root.addView(save)

        val privacy = TextView(this).apply {
            text = "Deze app leest geen gezondheidsgegevens. Hij vraagt alleen toestemming om mindfulness-sessies te schrijven."
            setPadding(0, 24, 0, 0)
        }
        root.addView(privacy)

        setContentView(root)
    }

    private fun saveMeditation(minutes: Int) {
        lifecycleScope.launch {
            val granted = healthConnectClient.permissionController.getGrantedPermissions()
            if (permission !in granted) {
                requestPermission.launch(setOf(permission))
                return@launch
            }

            val end = Instant.now()
            val start = end.minusSeconds(minutes * 60L)
            val record = MindfulnessSessionRecord(
                startTime = start,
                startZoneOffset = null,
                endTime = end,
                endZoneOffset = null,
                mindfulnessSessionType = MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MEDITATION,
                title = "Meditatie",
                notes = null,
                metadata = Metadata.manualEntry()
            )
            healthConnectClient.insertRecords(listOf(record))
            Toast.makeText(
                this@MainActivity,
                "$minutes minuten meditatie opgeslagen",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
