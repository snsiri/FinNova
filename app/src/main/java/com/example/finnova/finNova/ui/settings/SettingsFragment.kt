package com.example.finnova.finNova.ui.settings

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.finnova.R
import com.example.finnova.databinding.FragmentSettingsBinding
import com.example.finnova.finNova.data.PreferenceManager
import com.example.finnova.finNova.data.Transaction
import com.example.finnova.finNova.util.BackupUtil
import com.example.finnova.finNova.util.DataExportUtil
import com.example.finnova.finNova.util.ReminderManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var viewModel: SettingsViewModel
    private lateinit var dataExportUtil: DataExportUtil
    private lateinit var backupUtil: BackupUtil
    private lateinit var reminderManager: ReminderManager

    private val exportJsonLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                if (dataExportUtil.exportToJson(uri)) {
                    Toast.makeText(requireContext(), "Data exported successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Failed to export data", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private val exportTextLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                if (dataExportUtil.exportToText(uri)) {
                    Toast.makeText(requireContext(), "Data exported successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Failed to export data", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private val backupLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val transactions = preferenceManager.getTransactions()
                val monthlyBudget = preferenceManager.getMonthlyBudget()
                val currency = preferenceManager.getSelectedCurrency()
                
                if (backupUtil.createBackup(uri, transactions, monthlyBudget, currency)) {
                    Toast.makeText(requireContext(), R.string.msg_backup_success, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), R.string.msg_error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private val restoreLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val backupData = backupUtil.restoreFromBackup(uri)
                if (backupData != null) {
                    try {
                        backupData["currency"]?.let { currency ->
                            preferenceManager.setSelectedCurrency(currency.toString())
                        }
                        backupData["budget"]?.let { budget ->
                            preferenceManager.saveMonthlyBudget((budget as Number).toDouble())
                        }
                        backupData["transactions"]?.let { transactions ->
                            val gson = Gson()
                            val json = gson.toJson(transactions)
                            val type = object : TypeToken<List<Transaction>>() {}.type
                            val transactionList = gson.fromJson<List<Transaction>>(json, type)
                            preferenceManager.saveTransactions(transactionList)
                        }
                        
                        Toast.makeText(requireContext(), R.string.msg_restore_success, Toast.LENGTH_SHORT).show()
                        // Refresh UI
                        setupUI()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(requireContext(), R.string.msg_error, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), R.string.msg_error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        preferenceManager = PreferenceManager(requireContext())
        viewModel = ViewModelProvider(this, SettingsViewModelFactory(preferenceManager))
            .get(SettingsViewModel::class.java)
        dataExportUtil = DataExportUtil(requireContext())
        backupUtil = BackupUtil(requireContext())
        reminderManager = ReminderManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupClickListeners()
        observeViewModel()
        
        // Set initial dark mode state
        binding.switchDarkMode.isChecked = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
    }

    private fun setupUI() {
        // Setup currency spinner
        val currencies = resources.getStringArray(R.array.currencies)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, currencies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCurrency.setAdapter(adapter)
            
            // Set current currency
            val currentCurrency = preferenceManager.getSelectedCurrency()
        val currencyIndex = currencies.indexOf(currentCurrency)
            if (currencyIndex != -1) {
            binding.spinnerCurrency.setText(currentCurrency, false)
            }
            
        // Setup dark mode switch
        binding.switchDarkMode.isChecked = preferenceManager.isDarkModeEnabled()

        // Setup reminder controls
        binding.switchReminder.isChecked = preferenceManager.isReminderEnabled()
        binding.btnReminderTime.text = preferenceManager.getReminderTime()
        binding.btnReminderTime.isEnabled = preferenceManager.isReminderEnabled()
    }

    private fun setupClickListeners() {
        binding.btnSaveCurrency.setOnClickListener {
            val selectedCurrency = binding.spinnerCurrency.text.toString()
            preferenceManager.setSelectedCurrency(selectedCurrency)
            Toast.makeText(requireContext(), "Currency updated", Toast.LENGTH_SHORT).show()
            }

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            preferenceManager.setDarkMode(isChecked)
            activity?.recreate()
        }

        binding.btnExportJson.setOnClickListener {
            exportJson()
        }

        binding.btnExportText.setOnClickListener {
            exportText()
        }

        binding.btnBackup.setOnClickListener {
            createBackup()
        }

        binding.btnRestore.setOnClickListener {
            restoreBackup()
        }

        binding.switchReminder.setOnCheckedChangeListener { _, isChecked ->
            preferenceManager.setReminderEnabled(isChecked)
            binding.btnReminderTime.isEnabled = isChecked
            
            if (isChecked) {
                reminderManager.scheduleReminder(preferenceManager.getReminderTime())
                } else {
                reminderManager.cancelReminder()
            }
        }

        binding.btnReminderTime.setOnClickListener {
            showTimePickerDialog()
        }
    }

    private fun observeViewModel() {
        // Implement the logic to observe the ViewModel
    }

    private fun exportJson() {
        val dateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, "finnova_transactions_$dateStr.json")
        }
        exportJsonLauncher.launch(intent)
    }

    private fun exportText() {
        val dateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, "finnova_transactions_$dateStr.txt")
        }
        exportTextLauncher.launch(intent)
    }

    private fun createBackup() {
        val dateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, "finnova_backup_$dateStr.json")
        }
        backupLauncher.launch(intent)
    }

    private fun restoreBackup() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
        restoreLauncher.launch(intent)
    }

    private fun showTimePickerDialog() {
        val currentTime = preferenceManager.getReminderTime()
        val (hours, minutes) = currentTime.split(":").map { it.toInt() }

        TimePickerDialog(
            requireContext(),
            { _, selectedHour, selectedMinute ->
                val newTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                preferenceManager.setReminderTime(newTime)
                binding.btnReminderTime.text = newTime
                
                if (preferenceManager.isReminderEnabled()) {
                    reminderManager.scheduleReminder(newTime)
                }
            },
            hours,
            minutes,
            true
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 