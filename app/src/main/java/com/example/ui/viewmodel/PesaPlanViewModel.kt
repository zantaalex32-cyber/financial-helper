package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.BudgetLimitEntity
import com.example.data.model.MonthlySummary
import com.example.data.model.ThresholdAlert
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.parser.MpesaSmsParser
import com.example.data.repository.PesaPlanRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

sealed interface SmsSyncState {
    data object Idle : SmsSyncState
    data object Syncing : SmsSyncState
    data class Success(val newCount: Int, val message: String) : SmsSyncState
    data class Error(val message: String) : SmsSyncState
}

class PesaPlanViewModel(private val repository: PesaPlanRepository) : ViewModel() {

    private val calendar = Calendar.getInstance()
    private val _selectedYear = MutableStateFlow(calendar.get(Calendar.YEAR))
    private val _selectedMonth = MutableStateFlow(calendar.get(Calendar.MONTH)) // 0-based

    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()
    val selectedMonth: StateFlow<Int> = _selectedMonth.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow("All")
    val selectedCategoryFilter: StateFlow<String> = _selectedCategoryFilter.asStateFlow()

    private val _smsSyncState = MutableStateFlow<SmsSyncState>(SmsSyncState.Idle)
    val smsSyncState: StateFlow<SmsSyncState> = _smsSyncState.asStateFlow()

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgetLimits: StateFlow<List<BudgetLimitEntity>> = repository.allBudgetLimits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val monthlySummary: StateFlow<MonthlySummary> = combine(_selectedYear, _selectedMonth) { year, month ->
        Pair(year, month)
    }.flatMapLatest { (year, month) ->
        repository.getMonthlySummaryFlow(year, month)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthlySummary("2026-10", "October 2026"))

    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions,
        _searchQuery,
        _selectedCategoryFilter,
        _selectedYear,
        _selectedMonth
    ) { txList, query, catFilter, year, month ->
        val cal = Calendar.getInstance()
        txList.filter { tx ->
            cal.timeInMillis = tx.timestamp
            val txYear = cal.get(Calendar.YEAR)
            val txMonth = cal.get(Calendar.MONTH)
            val matchesMonth = (txYear == year && txMonth == month)

            val matchesQuery = query.isBlank() ||
                    tx.recipientOrSender.contains(query, ignoreCase = true) ||
                    tx.code.contains(query, ignoreCase = true) ||
                    tx.accountOrDetails.contains(query, ignoreCase = true) ||
                    tx.category.contains(query, ignoreCase = true)

            val matchesCategory = catFilter == "All" || tx.category.equals(catFilter, ignoreCase = true)

            matchesMonth && matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(category: String) {
        _selectedCategoryFilter.value = category
    }

    fun previousMonth() {
        if (_selectedMonth.value == 0) {
            _selectedMonth.value = 11
            _selectedYear.value -= 1
        } else {
            _selectedMonth.value -= 1
        }
    }

    fun nextMonth() {
        if (_selectedMonth.value == 11) {
            _selectedMonth.value = 0
            _selectedYear.value += 1
        } else {
            _selectedMonth.value += 1
        }
    }

    fun resetToCurrentMonth() {
        val now = Calendar.getInstance()
        _selectedYear.value = now.get(Calendar.YEAR)
        _selectedMonth.value = now.get(Calendar.MONTH)
    }

    fun syncDeviceSms(context: Context) {
        viewModelScope.launch {
            _smsSyncState.value = SmsSyncState.Syncing
            try {
                val newCount = repository.syncInboxSms(context)
                _smsSyncState.value = SmsSyncState.Success(
                    newCount = newCount,
                    message = if (newCount > 0)
                        "Synced $newCount new M-Pesa transaction${if (newCount > 1) "s" else ""} from SMS inbox!"
                    else
                        "All M-Pesa SMS are already up to date."
                )
            } catch (e: Exception) {
                _smsSyncState.value = SmsSyncState.Error("SMS sync error: ${e.localizedMessage ?: "Permission required"}")
            }
        }
    }

    fun clearSyncStatus() {
        _smsSyncState.value = SmsSyncState.Idle
    }

    fun parseAndAddSms(smsText: String): String? {
        val parsed = MpesaSmsParser.parse(smsText)
        return if (parsed != null) {
            viewModelScope.launch {
                repository.insertTransaction(parsed)
            }
            null // Success, no error
        } else {
            "Could not parse valid M-Pesa message. Ensure message starts with a 10-char code (e.g. QA12BC34DE) and has 'Confirmed. Ksh...'."
        }
    }

    fun addManualTransaction(
        recipient: String,
        amount: Double,
        category: String,
        type: TransactionType,
        cost: Double = 0.0,
        notes: String = "",
        isExpense: Boolean = true
    ) {
        viewModelScope.launch {
            val randomCode = "MAN" + UUID.randomUUID().toString().replace("-", "").take(7).uppercase()
            val tx = TransactionEntity(
                code = randomCode,
                type = type.name,
                recipientOrSender = recipient,
                accountOrDetails = "Manual Entry",
                amount = amount,
                cost = cost,
                balance = null,
                timestamp = System.currentTimeMillis(),
                category = category,
                rawSms = "Manual entry for $recipient",
                isExpense = isExpense,
                notes = notes
            )
            repository.insertTransaction(tx)
        }
    }

    fun updateTransactionCategory(id: Long, newCategory: String) {
        viewModelScope.launch {
            repository.updateTransactionCategory(id, newCategory)
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
        }
    }

    fun updateBudgetLimit(
        category: String,
        limit: Double,
        thresholdPercent: Int = 80,
        isEnabled: Boolean = true
    ) {
        viewModelScope.launch {
            val existing = budgetLimits.value.find { it.category == category }
            val meta = TransactionCategory.DEFAULT_CATEGORIES.find { it.id == category }
            val colorHex = existing?.colorHex ?: meta?.colorHex ?: "#10B981"
            val iconKey = existing?.iconKey ?: meta?.iconKey ?: "category"

            val updated = BudgetLimitEntity(
                category = category,
                monthlyLimit = limit,
                warningThresholdPercent = thresholdPercent,
                colorHex = colorHex,
                iconKey = iconKey,
                isEnabled = isEnabled
            )
            repository.saveBudgetLimit(updated)
        }
    }

    fun loadSampleData() {
        viewModelScope.launch {
            repository.resetWithSampleData()
            _smsSyncState.value = SmsSyncState.Success(15, "Loaded 15 authentic Kenyan M-Pesa sample transactions!")
        }
    }
}

class PesaPlanViewModelFactory(private val repository: PesaPlanRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PesaPlanViewModel::class.java)) {
            return PesaPlanViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
