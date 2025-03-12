package com.example.test_health_connect

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.health.connect.HealthConnectManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectClient.Companion.SDK_AVAILABLE
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.navigation.fragment.findNavController
import com.example.test_health_connect.databinding.FragmentFirstBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import androidx.core.net.toUri

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {
    val TAG = FirstFragment::class.java.simpleName
    lateinit var healthConnectClient: HealthConnectClient
    private var _binding: FragmentFirstBinding? = null

    // The minimum android level that can use Health Connect
    val MIN_SUPPORTED_SDK = Build.VERSION_CODES.O_MR1 // 27
    private fun isSupported() = Build.VERSION.SDK_INT >= MIN_SUPPORTED_SDK

    val permissions = setOf(
        HealthPermission.getWritePermission(WeightRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
    )
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    private suspend fun hasAllPermissions(permissions: Set<String>): Boolean {
        return healthConnectClient.permissionController.getGrantedPermissions().containsAll(permissions)
    }

    private fun requestPermissionsActivityContract(): ActivityResultContract<Set<String>, Set<String>> {
        return PermissionController.createRequestPermissionResultContract()
    }

    private val requestPermissionLauncher =
        registerForActivityResult(requestPermissionsActivityContract()) { grantedPermissions ->
            Log.e(TAG, "requestPermissionLauncher: $grantedPermissions")
        }

    /**
     * Convenience function to reuse code for reading data.
     */
    private suspend inline fun <reified T : Record> readData(
        timeRangeFilter: TimeRangeFilter,
        dataOriginFilter: Set<DataOrigin> = setOf(),
    ): List<T> {
        val request = ReadRecordsRequest(
            recordType = T::class,
            dataOriginFilter = dataOriginFilter,
            timeRangeFilter = timeRangeFilter
        )
        return healthConnectClient.readRecords(request).records
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /// will cause error when delete health connect app
        healthConnectClient =
            HealthConnectClient.getOrCreate(view.context)

        binding.btnCheckPermission.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                coroutineScope {
                    val result = hasAllPermissions(permissions)
                    Log.e(TAG, "btnCheckPermission: $result")
                }
            }

        }
        binding.btnRequestPermission.setOnClickListener {
            Log.e(TAG, "btnRequestPermission: pressed")
            val request = requestPermissionsActivityContract()
            requestPermissionLauncher.launch(permissions)

        }
        binding.btnOpenGoogleHealthConnectApp.setOnClickListener {
            //
        }
        binding.checkHealthConnectIsDownloaded.setOnClickListener {
            val sdkAvailable = HealthConnectClient.getSdkStatus(requireContext())
            Log.e(TAG, "checkHealthConnectIsDownloaded: $sdkAvailable,  SDK_AVAILABLE: $SDK_AVAILABLE")
        }
        binding.readData.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                coroutineScope {
                    val startOfDay = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS)
                    val now = Instant.now()
                    val endOfWeek = startOfDay.toInstant().plus(7, ChronoUnit.DAYS)
                    val request = ReadRecordsRequest(
                        recordType = WeightRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startOfDay.toInstant(), now)
                    )
                    val result = healthConnectClient.readRecords(
                        request = request
                    )

                    Log.e(TAG, "readData: ${result.records}")
                }
            }

        }

        binding.writeData.setOnClickListener {
            //
        }

        binding.btnCheckDeviceSupport.setOnClickListener {
            val isSupport = isSupported()
            Log.e(TAG, "btnCheckDeviceSupport: $isSupport")
        }

        binding.btnOpenGoogleHealthConnectAppStore.setOnClickListener {
            openHealthConnectOnPlayStore(requireContext())
        }

    }

    private fun openHealthConnectOnPlayStore(context: Context) {
        val packageName = "com.google.android.apps.healthdata" // Health Connect package name
        val marketUrl = "market://details?id=$packageName" // Play Store app URL
        val webUrl = "https://play.google.com/store/apps/details?id=$packageName" // Web fallback URL

        try {
            // Try opening Play Store app
            context.startActivity(Intent(Intent.ACTION_VIEW, marketUrl.toUri()).apply {
                setPackage("com.android.vending") // Ensure it opens in Play Store
            })
        } catch (e: ActivityNotFoundException) {
            // If Play Store is not installed, open in a web browser
            context.startActivity(Intent(Intent.ACTION_VIEW, webUrl.toUri()))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}