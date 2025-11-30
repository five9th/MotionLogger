package com.five9th.motionlogger.presentation

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.five9th.motionlogger.R
import com.five9th.motionlogger.databinding.ActivityMainBinding
import com.five9th.motionlogger.domain.entities.SensorsInfo

class MainActivity : AppCompatActivity() {

    private val tag = "MainActivity"

    private lateinit var mainViewModel: MainViewModel

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViewModel()
    }

    private fun initViewModel() {
        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]
        mainViewModel.sensorsInfoLD.observe(this, ::onSensorsInfoChanged)

        mainViewModel.getSensorsInfo()
    }


    private fun onSensorsInfoChanged(sensorsInfo: SensorsInfo) {
        val strYes = getString(R.string.yes)
        val strNo = getString(R.string.no)

        binding.tvHasAccelerometer.text = if (sensorsInfo.hasAccelerometer) strYes else strNo
        binding.tvHasGyroscope.text = if (sensorsInfo.hasGyroscope) strYes else strNo
        binding.tvHasMagnetometer.text = if (sensorsInfo.hasMagnetometer) strYes else strNo
        binding.tvHasGameRotationVector.text = if (sensorsInfo.hasGameRotationVector) strYes else strNo
        binding.tvHasRotationVector.text = if (sensorsInfo.hasRotationVector) strYes else strNo
    }
}