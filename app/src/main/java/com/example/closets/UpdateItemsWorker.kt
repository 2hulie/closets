package com.example.closets

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.closets.repository.AppDatabase
import com.example.closets.repository.ItemRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UpdateItemsWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    private val repository: ItemRepository = ItemRepository(AppDatabase.getDatabase(appContext).itemDao())

    override fun doWork(): Result {
        return runBlocking { // Create a coroutine scope
            try {
                Log.d("UpdateItemsWorker", "Updating worn times and last worn dates for checked items.")
                updateCheckedItems() // Call the suspend function
                Result.success()
            } catch (e: Exception) {
                Log.e("UpdateItemsWorker", "Error updating items: ${e.message}", e)
                Result.failure()
            }
        }
    }

    private suspend fun updateCheckedItems() {
        withContext(Dispatchers.IO) {
            val currentDate = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())
            val allItems = repository.getAllItemsDirectly() // Fetch all items directly
            val checkedItems = allItems.filter { it.isChecked } // Filter checked items

            checkedItems.forEach { item ->
                item.wornTimes += 1
                item.lastWornDate = currentDate
                repository.updateItem(item) // Update each item in the database
            }
        }
    }
}