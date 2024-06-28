package com.example.messanger

data class User(
    var uid: String = "",
    var lastName: String = "",
    var firstName: String = "",
    var nickname: String = "",
    var email: String = "",
    var password: String = "",
    var friendRequests: List<String> = listOf(),
    var friends: List<String> = listOf()
)