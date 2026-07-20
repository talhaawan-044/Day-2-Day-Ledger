package com.example.awancoalledger.viewmodel.features

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.awancoalledger.data.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotesViewModel(
    private val repository: LedgerRepository,
    private val syncManager: SyncManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    
    private val _isGridView = kotlinx.coroutines.flow.MutableStateFlow(settingsRepository.isNotesGridView())
    val isGridView: kotlinx.coroutines.flow.StateFlow<Boolean> = _isGridView
    fun toggleGridView(isGrid: Boolean) { _isGridView.value = isGrid; settingsRepository.setNotesGridView(isGrid) }

    val allNotes = repository.getAllNotes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allFolders = repository.getAllFolders().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addFolder(name: String) {
        viewModelScope.launch {
            val folder = Folder(name = name)
            repository.insertFolder(folder)
            syncManager.uploadFolder(folder)
        }
    }

    fun updateFolder(folder: Folder) {
        viewModelScope.launch {
            val updated = folder.copy(lastUpdated = System.currentTimeMillis())
            repository.insertFolder(updated)
            syncManager.uploadFolder(updated)
        }
    }

    fun deleteFolder(folder: Folder) {
        viewModelScope.launch {
            val deleted = folder.copy(isDeleted = true, lastUpdated = System.currentTimeMillis())
            repository.insertFolder(deleted)
            syncManager.uploadFolder(deleted)
            syncManager.deleteFolder(deleted)

            // Also delete notes in this folder
            val notesInFolder = repository.getNotesInFolder(folder.id).stateIn(viewModelScope).value
            notesInFolder.forEach { note ->
                deleteNote(note)
            }
        }
    }

    fun addNote(title: String, content: String, color: Int? = null, textColor: Int? = null, fontSize: Float? = null, bgImageId: Int? = null, isPinned: Boolean = false, folderId: Int? = null) {
        viewModelScope.launch {
            val note = Note(title = title, content = content, color = color, textColor = textColor, fontSize = fontSize, bgImageId = bgImageId, isPinned = isPinned, folderId = folderId)
            repository.insertNote(note)
            syncManager.uploadNote(note)
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch {
            val updated = note.copy(lastUpdated = System.currentTimeMillis())
            repository.updateNote(updated)
            syncManager.uploadNote(updated)
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            val deleted = note.copy(isDeleted = true, lastUpdated = System.currentTimeMillis())
            repository.updateNote(deleted)
            syncManager.uploadNote(deleted)
            syncManager.deleteNote(deleted)
        }
    }
}
