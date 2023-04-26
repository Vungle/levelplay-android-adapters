package com.vungle.ironsource.app

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.utils.IronSourceUtils
import com.vungle.ironsource.app.databinding.ActivityStartupBinding

class StartupActivity : AppCompatActivity() {

    private companion object {
        private const val NON_BIDDING_APP_KEY = "16bf53535"
        private const val BIDDING_APP_KEY = "17993777d"
        private const val NON_BIDDING_MREC_APP_KEY = "17a70d3c5"
        private const val BIDDING_MREC_APP_KEY = "17a71805d"
    }

    private lateinit var binding: ActivityStartupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStartupBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val adapterVer = com.ironsource.adapters.vungle.BuildConfig.VERSION_NAME
        val vngSdkVer = getVngSdkVersion()

        binding.versionTxt.text =
                resources.getString(R.string.version, IronSourceUtils.getSDKVersion(), adapterVer, vngSdkVer, BuildConfig.VERSION_NAME)

        binding.btnGdpr.setOnClickListener {
            onCreateDialog("GDPR").show()
        }
        binding.btnCcpa.setOnClickListener {
            onCreateDialog("CCPA").show()
        }
        binding.btnCoppa.setOnClickListener {
            onCreateDialog("COPPA").show()
        }
        binding.nonBiddingTestBtn.setOnClickListener {
            val intent = Intent(this, DemoActivity::class.java)
            intent.putExtra("APP_KEY", NON_BIDDING_APP_KEY)
            startActivity(intent)
        }
        binding.biddingTestBtn.setOnClickListener {
            val intent = Intent(this, DemoActivity::class.java)
            intent.putExtra("APP_KEY", BIDDING_APP_KEY)
            startActivity(intent)
        }
        binding.nonBiddingMrecTestBtn.setOnClickListener {
            val intent = Intent(this, DemoMrecActivity::class.java)
            intent.putExtra("APP_KEY", NON_BIDDING_MREC_APP_KEY)
            startActivity(intent)
        }
        binding.biddingMrecTestBtn.setOnClickListener {
            val intent = Intent(this, DemoMrecActivity::class.java)
            intent.putExtra("APP_KEY", BIDDING_MREC_APP_KEY)
            startActivity(intent)
        }
    }

    private fun onCreateDialog(consentType: String): Dialog {
        return this.let {
            val builder = AlertDialog.Builder(it)
            builder.setTitle("$consentType Consent")
                    .setItems(R.array.consent_options) { _, which ->
                        // The 'which' argument contains the index position
                        // of the selected item
                        when (consentType) {
                            "COPPA" -> {
                                // If the end-user is not a child: IronSource.setMetaData("is_child_directed","false")
                                // Vungle mediation: https://developers.is.com/ironsource-mobile/android/vungle-mediation-guide/#step-5
                                IronSource.setMetaData("Vungle_coppa", (which == 0).toString().lowercase())
                            }
                            "CCPA" -> {
                                // If “sale” of personal information is permitted: IronSource.setMetaData("do_not_sell","false")
                                IronSource.setMetaData("do_not_sell", (which == 1).toString().lowercase())
                            }
                            "GDPR" -> {
                                // If the user provided consent, please set the following flag to true:
                                IronSource.setConsent(which == 0)
                            }
                        }
                    }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun getVngSdkVersion(): String {
        try {
            val cls = Class.forName("com.vungle.ads.BuildConfig")
            val field = cls.getDeclaredField("VERSION_NAME")
            field.isAccessible = true
            return field[null] as String
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

}
