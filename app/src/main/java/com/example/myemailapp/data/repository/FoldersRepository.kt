package com.example.myemailapp.data.repository

import com.example.myemailapp.domain.model.Folder

interface FoldersRepository {
    suspend fun getFolders(): Result<List<Folder>>
    suspend fun createFolder(folder: Folder): Result<String>
    suspend fun updateFolder(folder: Folder): Result<Unit>
    suspend fun deleteFolder(folderId: String): Result<Unit>
    suspend fun getFolderById(folderId: String): Result<Folder>
    suspend fun getChildFolders(parentId: String): Result<List<Folder>>
}