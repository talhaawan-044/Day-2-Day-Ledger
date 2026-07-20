package com.example.awancoalledger.viewmodel.features

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.awancoalledger.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.absoluteValue

@OptIn(ExperimentalCoroutinesApi::class)
class VehicleViewModel(
    private val repository: LedgerRepository,
    private val syncManager: SyncManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val allVehicles = repository.getAllVehicles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedVehicleId = MutableStateFlow<Int?>(null)
    val selectedVehicleId: StateFlow<Int?> = _selectedVehicleId.asStateFlow()

    fun selectVehicle(id: Int?) { _selectedVehicleId.value = id }

    val selectedVehicle: StateFlow<Vehicle?> = _selectedVehicleId.flatMapLatest { id ->
        if (id == null) kotlinx.coroutines.flow.flowOf(null) else allVehicles.map { list -> list.find { it.id == id } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allFuelEntries: StateFlow<List<FuelEntry>> = _selectedVehicleId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getFuelEntriesForVehicle(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMaintenanceEntries: StateFlow<List<MaintenanceEntry>> = _selectedVehicleId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getMaintenanceEntriesForVehicle(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestFuelEntry: StateFlow<FuelEntry?> = allFuelEntries.map { entries ->
        entries.maxByOrNull { it.mileage }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val oilChangeInterval = settingsRepository.getSettingsFlow()
        .map { settingsRepository.getOilChangeInterval().toDouble() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3000.0)

    val lastOilChangeMileage = allMaintenanceEntries.map { entries ->
        entries.filter { it.type == "OIL_CHANGE" }.firstOrNull()?.mileage ?: 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val nextOilChangeMileage = kotlinx.coroutines.flow.combine(lastOilChangeMileage, oilChangeInterval) { mileage, interval ->
        if (mileage > 0) mileage + interval else 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val currentVehicleMileage = kotlinx.coroutines.flow.combine(latestFuelEntry, allMaintenanceEntries, selectedVehicle) { fuel, maint, vehicle ->
        val maxFuel = fuel?.mileage ?: 0.0
        val maxMaint = maint.maxOfOrNull { it.mileage } ?: 0.0
        val maxVehicle = vehicle?.currentMileage ?: 0.0
        maxOf(maxFuel, maxMaint, maxVehicle)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val kmsRemainingForOilChange = kotlinx.coroutines.flow.combine(nextOilChangeMileage, currentVehicleMileage) { next, current ->
        if (next > 0) next - current else 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val fuelEfficiencyTrend: StateFlow<List<Float>> = allFuelEntries.map { entries ->
        if (entries.size < 2) return@map emptyList<Float>()
        val recent = entries.take(5)
        val floats = mutableListOf<Float>()
        for (i in 0 until recent.size - 1) {
            val current = recent[i]
            val previous = recent[i+1]
            val totalKm = (current.mileage - previous.mileage).absoluteValue
            val totalLiters = current.liters
            val eff = if (totalLiters > 0) totalKm / totalLiters else 0.0
            floats.add(eff.toFloat())
        }
        floats.reversed()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyFuelCost: StateFlow<Double> = allFuelEntries.map { entries ->
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)
        entries.filter {
            cal.timeInMillis = it.date
            cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
        }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val monthlyMaintenanceCost: StateFlow<Double> = allMaintenanceEntries.map { entries ->
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)
        entries.filter {
            cal.timeInMillis = it.date
            cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
        }.sumOf { it.cost }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    
    private val _vehicleAlert = MutableStateFlow<String?>(null)
    val vehicleAlert: StateFlow<String?> = _vehicleAlert.asStateFlow()
    fun dismissVehicleAlert() { _vehicleAlert.value = null }

    fun addVehicle(name: String, registration: String, type: String, initialMileage: Double = 0.0, isPrimary: Boolean = false) {
        viewModelScope.launch {
            val v = Vehicle(name = name, plateNumber = registration, type = type, currentMileage = initialMileage, isPrimary = isPrimary)
            repository.insertVehicle(v)
            syncManager.uploadVehicle(v)
        }
    }

    fun updateVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            val updated = vehicle.copy(lastUpdated = System.currentTimeMillis())
            repository.insertVehicle(updated)
            syncManager.uploadVehicle(updated)
        }
    }

    fun deleteVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            val deleted = vehicle.copy(isDeleted = true, lastUpdated = System.currentTimeMillis())
            repository.insertVehicle(deleted)
            syncManager.uploadVehicle(deleted)
            syncManager.deleteVehicle(deleted)
        }
    }

    fun setPrimaryVehicle(vehicleId: Int) {
        viewModelScope.launch {
            val allVehiclesList = repository.getAllVehicles().firstOrNull() ?: emptyList()
            allVehiclesList.forEach { v ->
                val isPrimary = v.id == vehicleId
                if (v.isPrimary != isPrimary) {
                    val updated = v.copy(isPrimary = isPrimary, lastUpdated = System.currentTimeMillis())
                    repository.insertVehicle(updated)
                    syncManager.uploadVehicle(updated)
                }
            }
        }
    }

    fun addFuelEntry(mileage: Double, liters: Double, amount: Double, date: Long = System.currentTimeMillis()) {
        val vId = _selectedVehicleId.value ?: return
        viewModelScope.launch {
            val entry = FuelEntry(vehicleId = vId, mileage = mileage, liters = liters, amount = amount, date = date)
            repository.insertFuelEntry(entry)
            syncManager.uploadFuelEntry(entry)

            selectedVehicle.value?.let { v ->
                if (mileage > v.currentMileage) {
                    val updated = v.copy(currentMileage = mileage, lastUpdated = System.currentTimeMillis())
                    repository.insertVehicle(updated)
                    syncManager.uploadVehicle(updated)
                }
            }
        }
    }

    fun updateFuelEntry(entry: FuelEntry) {
        viewModelScope.launch {
            val updated = entry.copy(lastUpdated = System.currentTimeMillis())
            repository.insertFuelEntry(updated)
            syncManager.uploadFuelEntry(updated)

            selectedVehicle.value?.let { v ->
                if (entry.mileage > v.currentMileage) {
                    val upd = v.copy(currentMileage = entry.mileage, lastUpdated = System.currentTimeMillis())
                    repository.insertVehicle(upd)
                    syncManager.uploadVehicle(upd)
                }
            }
        }
    }

    fun deleteFuelEntry(entry: FuelEntry) {
        viewModelScope.launch {
            val deleted = entry.copy(isDeleted = true, lastUpdated = System.currentTimeMillis())
            repository.insertFuelEntry(deleted)
            syncManager.uploadFuelEntry(deleted)
            syncManager.deleteFuelEntry(deleted)
            recalculateVehicleMileage(entry.vehicleId)
        }
    }

    fun addMaintenanceEntry(mileage: Double, cost: Double, description: String, isOilChange: Boolean, date: Long = System.currentTimeMillis()) {
        val vId = _selectedVehicleId.value ?: return
        viewModelScope.launch {
            val entry = MaintenanceEntry(
                vehicleId = vId,
                mileage = mileage,
                cost = cost,
                description = description,
                type = if (isOilChange) "OIL_CHANGE" else "OTHER",
                date = date
            )
            repository.insertMaintenanceEntry(entry)
            syncManager.uploadMaintenanceEntry(entry)

            selectedVehicle.value?.let { v ->
                if (mileage > v.currentMileage) {
                    val updated = v.copy(currentMileage = mileage, lastUpdated = System.currentTimeMillis())
                    repository.insertVehicle(updated)
                    syncManager.uploadVehicle(updated)
                }
            }
        }
    }

    fun updateMaintenanceEntry(entry: MaintenanceEntry) {
        viewModelScope.launch {
            val updated = entry.copy(lastUpdated = System.currentTimeMillis())
            repository.insertMaintenanceEntry(updated)
            syncManager.uploadMaintenanceEntry(updated)

            selectedVehicle.value?.let { v ->
                if (entry.mileage > v.currentMileage) {
                    val upd = v.copy(currentMileage = entry.mileage, lastUpdated = System.currentTimeMillis())
                    repository.insertVehicle(upd)
                    syncManager.uploadVehicle(upd)
                }
            }
        }
    }

    fun deleteMaintenanceEntry(entry: MaintenanceEntry) {
        viewModelScope.launch {
            val deleted = entry.copy(isDeleted = true, lastUpdated = System.currentTimeMillis())
            repository.insertMaintenanceEntry(deleted)
            syncManager.uploadMaintenanceEntry(deleted)
            syncManager.deleteMaintenanceEntry(deleted)
            recalculateVehicleMileage(entry.vehicleId)
        }
    }

    private suspend fun recalculateVehicleMileage(vehicleId: Int) {
        val fuelEntries = repository.getAllFuelEntriesList().filter { it.vehicleId == vehicleId && !it.isDeleted }
        val maintEntries = repository.getAllMaintenanceEntriesList().filter { it.vehicleId == vehicleId && !it.isDeleted }

        val maxFuel = fuelEntries.maxOfOrNull { it.mileage } ?: 0.0
        val maxMaint = maintEntries.maxOfOrNull { it.mileage } ?: 0.0
        val highestEntryMileage = maxOf(maxFuel, maxMaint)

        repository.getVehicleById(vehicleId)?.let { vehicle ->
            if (highestEntryMileage > 0 && highestEntryMileage < vehicle.currentMileage) {
                val updated = vehicle.copy(currentMileage = highestEntryMileage, lastUpdated = System.currentTimeMillis())
                repository.insertVehicle(updated)
                syncManager.uploadVehicle(updated)
            }
        }
    }
    val avgKmPerLiter = allVehicles.map { vehicles ->
        // placeholder or actual logic for avg km
        0.0 
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
}
