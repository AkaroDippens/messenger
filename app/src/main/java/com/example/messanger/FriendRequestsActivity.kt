package com.example.messanger

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun FriendRequestsScreen(navController: NavHostController) {
    var friendRequests by remember { mutableStateOf(listOf<User>()) }
    val context = LocalContext.current
    val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

    LaunchedEffect(currentUserUid) {
        fetchFriendRequests(context, currentUserUid) { results ->
            friendRequests = results
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        LazyColumn {
            items(friendRequests) { user ->
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(text = user.nickname)
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = { acceptFriendRequest(context, currentUserUid, user.uid) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Принять")
                    }
                }
            }
        }

        if (friendRequests.isEmpty()) {
            Text("Нет запросов в друзья")
        }
    }
}

private fun fetchFriendRequests(context: Context, currentUserUid: String, onResults: (List<User>) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val userCollection = db.collection("users")
    val userQuery = userCollection.whereEqualTo("uid", currentUserUid)

    userQuery.get()
        .addOnSuccessListener { querySnapshot ->
            if (querySnapshot.documents.isNotEmpty()) {
                val documentSnapshot = querySnapshot.documents[0]
                val user = documentSnapshot.toObject(User::class.java)
                if (user != null) {
                    val requests = user.friendRequests
                    if (requests.isNotEmpty()) {
                        userCollection.whereIn("uid", requests).get()
                            .addOnSuccessListener { requestSnapshot ->
                                val results = requestSnapshot.documents.mapNotNull { document ->
                                    document.toObject(User::class.java)
                                }
                                onResults(results)
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Не удалось загрузить запросы в друзья. Пожалуйста, попробуйте позже.", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        onResults(emptyList())
                    }
                } else {
                    Toast.makeText(context, "Не удалось найти данные пользователя.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Пользователь не найден.", Toast.LENGTH_SHORT).show()
            }
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Ошибка при загрузке данных. Пожалуйста, попробуйте позже.", Toast.LENGTH_SHORT).show()
        }
}

private fun acceptFriendRequest(context: Context, currentUserUid: String, friendUid: String) {
    val db = FirebaseFirestore.getInstance()

    val userCollection = db.collection("users")
    val currentUserQuery = userCollection.whereEqualTo("uid", currentUserUid)
    val friendUserQuery = userCollection.whereEqualTo("uid", friendUid)

    currentUserQuery.get()
        .addOnSuccessListener { currentUserSnapshot ->
            if (!currentUserSnapshot.isEmpty) {
                val currentUserDocument = currentUserSnapshot.documents[0]
                val currentUser = currentUserDocument.toObject(User::class.java)

                if (currentUser != null) {
                    val friendRequests = currentUser.friendRequests.toMutableList()
                    val currentUserFriends = currentUser.friends.toMutableList()

                    if (currentUserFriends.contains(friendUid)) {
                        Toast.makeText(context, "Этот пользователь уже у вас в друзьях.", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    if (friendRequests.contains(friendUid)) {
                        val updatedCurrentUserFriends = currentUserFriends.apply {
                            add(friendUid)
                        }
                        currentUserDocument.reference.update("friends", updatedCurrentUserFriends)
                        friendRequests.remove(friendUid)
                        currentUserDocument.reference.update("friendRequests", friendRequests)

                        friendUserQuery.get()
                            .addOnSuccessListener { friendUserSnapshot ->
                                if (!friendUserSnapshot.isEmpty) {
                                    val friendUserDocument = friendUserSnapshot.documents[0]
                                    val friendUser = friendUserDocument.toObject(User::class.java)

                                    if (friendUser != null) {
                                        val updatedFriendUserFriends = friendUser.friends.toMutableList().apply {
                                            add(currentUserUid)
                                        }
                                        friendUserDocument.reference.update("friends", updatedFriendUserFriends)
                                            .addOnSuccessListener {
                                                Toast.makeText(context, "Запрос в друзья принят.", Toast.LENGTH_SHORT).show()
                                            }
                                            .addOnFailureListener { e ->
                                                Toast.makeText(context, "Не удалось обновить список друзей. Пожалуйста, попробуйте позже.", Toast.LENGTH_SHORT).show()
                                            }
                                    } else {
                                        Toast.makeText(context, "Не удалось найти данные друга.", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Друг не найден.", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Ошибка при получении данных друга. Пожалуйста, попробуйте позже.", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(context, "Запрос в друзья не найден.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Не удалось найти данные текущего пользователя.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Текущий пользователь не найден.", Toast.LENGTH_SHORT).show()
            }
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Ошибка при принятии запроса в друзья. Пожалуйста, попробуйте позже.", Toast.LENGTH_SHORT).show()
        }
}
