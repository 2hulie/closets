package com.example.closets

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.closets.repository.AppDatabase
import com.example.closets.repository.ItemRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DateCheckWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    private val itemRepository: ItemRepository

    init {
        val database = AppDatabase.getDatabase(context)
        itemRepository = ItemRepository(database.itemDao())
    }

    override fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("CheckedItemsPrefs", Context.MODE_PRIVATE)
        val lastCheckDate = prefs.getString("LastCheckDate", null)
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        if (lastCheckDate != null && lastCheckDate != currentDate) {
            updateWornTimesAndLastWornDate(lastCheckDate)
            prefs.edit().putString("LastCheckDate", currentDate).apply()
        }

        return Result.success()
    }

    private fun updateWornTimesAndLastWornDate(lastCheckDate: String) {
        val checkedItemIds = applicationContext.getSharedPreferences("CheckedItemsPrefs", Context.MODE_PRIVATE)
            .getStringSet("CheckedItems", emptySet()) ?: emptySet()

        if (checkedItemIds.isNotEmpty()) {
            val formattedLastCheckDate = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                .format(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(lastCheckDate)!!)

            CoroutineScope(Dispatchers.IO).launch {
                val itemsToUpdate = itemRepository.getAllItemsDirectly() // Use the correct method
                    .filter { item -> checkedItemIds.contains(item.id.toString()) }

                itemsToUpdate.forEach { item ->
                    item.wornTimes += 1
                    item.lastWornDate = formattedLastCheckDate
                }

                itemRepository.updateItems(itemsToUpdate)
            }
        }
    }
}