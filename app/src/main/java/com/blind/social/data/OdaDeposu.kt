package com.blind.social.data

import com.blind.social.SupabaseModul
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.catch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive

class OdaDeposu {

    fun odalariGercekZamanliDinle(): Flow<Result<List<Oda>>> = flow {
        try {
            // First emit the initial state
            val initialOdalar = SupabaseModul.client.postgrest["odalar"]
                .select()
                .decodeList<Oda>()

            val currentList = initialOdalar.toMutableList()
            emit(Result.success(currentList.toList()))

            // Then listen for changes
            val channel = SupabaseModul.client.realtime.channel("odalar-changes")
            val changeFlow = channel.postgresChangeFlow<PostgresAction>("public") {
                table = "odalar"
            }
            channel.subscribe()

            changeFlow.collect { action ->
                when (action) {
                    is PostgresAction.Insert -> {
                        val newOda = Json { ignoreUnknownKeys = true }.decodeFromJsonElement<Oda>(action.record)
                        currentList.add(newOda)
                        emit(Result.success(currentList.toList()))
                    }
                    is PostgresAction.Update -> {
                        val updatedOda = Json { ignoreUnknownKeys = true }.decodeFromJsonElement<Oda>(action.record)
                        val index = currentList.indexOfFirst { it.id == updatedOda.id }
                        if (index != -1) {
                            currentList[index] = updatedOda
                            emit(Result.success(currentList.toList()))
                        }
                    }
                    is PostgresAction.Delete -> {
                        val deletedId = action.oldRecord["id"]?.jsonPrimitive?.let {
                            if (it.isString) it.content else it.content
                        }
                        if (deletedId != null) {
                            currentList.removeAll { it.id == deletedId }
                            emit(Result.success(currentList.toList()))
                        }
                    }
                    else -> {}
                }
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    suspend fun odaOlustur(oda: Oda): Result<Unit> {
        return try {
            SupabaseModul.client.postgrest["odalar"].insert(oda)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}