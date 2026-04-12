package com.blind.social.data

import com.blind.social.SupabaseModul
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.awaitClose
import android.util.Log
import io.github.jan.supabase.realtime.RealtimeChannel


class OdaDeposu {

    fun odalariGercekZamanliDinle(): Flow<Result<List<Oda>>> = callbackFlow {
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        var currentList = mutableListOf<Oda>()

        try {
            // First emit the initial state
            val initialOdalar = SupabaseModul.client.postgrest["odalar"]
                .select()
                .decodeList<Oda>()

            currentList = initialOdalar.toMutableList()
            trySend(Result.success(currentList.toList()))

            // Then listen for changes
            val channel = SupabaseModul.client.realtime.channel("odalar-changes")
            val changeFlow = channel.postgresChangeFlow<PostgresAction>("public") {
                table = "odalar"
            }

            coroutineScope.launch {
                channel.status.collect { status ->
                    Log.d("Realtime", "OdaDeposu channel status: $status")
                }
            }
            channel.subscribe()

            val job = coroutineScope.launch {
                changeFlow.collect { action ->
                    Log.d("Realtime", "OdaDeposu Postgres action: $action")
                    when (action) {
                        is PostgresAction.Insert -> {
                            val newOda = Json { ignoreUnknownKeys = true }.decodeFromJsonElement<Oda>(action.record)
                            currentList = currentList.toMutableList().apply { add(newOda) }
                            trySend(Result.success(currentList))
                        }
                        is PostgresAction.Update -> {
                            val updatedOda = Json { ignoreUnknownKeys = true }.decodeFromJsonElement<Oda>(action.record)
                            val index = currentList.indexOfFirst { it.id == updatedOda.id }
                            if (index != -1) {
                                currentList = currentList.toMutableList().apply { this[index] = updatedOda }
                                trySend(Result.success(currentList))
                            }
                        }
                        is PostgresAction.Delete -> {
                            val deletedId = action.oldRecord["id"]?.jsonPrimitive?.let {
                                if (it.isString) it.content else it.content
                            }
                            if (deletedId != null) {
                                currentList = currentList.toMutableList().apply { removeAll { it.id == deletedId } }
                                trySend(Result.success(currentList))
                            }
                        }
                        else -> {}
                    }
                }
            }

            awaitClose {
                job.cancel()
                coroutineScope.launch { channel.unsubscribe() }
            }
        } catch (e: Exception) {
            trySend(Result.failure(e))
            close(e)
        }
    }.flowOn(Dispatchers.IO)

    suspend fun odaOlustur(oda: Oda): Result<Unit> {
        return try {
            SupabaseModul.client.postgrest["odalar"].insert(oda)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}