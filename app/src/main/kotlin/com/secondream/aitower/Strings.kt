package com.secondream.aitower

enum class Lang { EN, IT }

data class Strings(
    val tapToStart: String,
    val tip: String,
    val floors: String,
    val best: String,
    val gameOver: String,
    val restart: String,
    val newBest: String,
    val settings: String,
    val sound: String,
    val vibration: String,
    val language: String,
    val close: String,
    val perfect: String
)

val EN = Strings(
    tapToStart = "TAP TO START",
    tip = "Tap to drop the block",
    floors = "FLOORS",
    best = "BEST",
    gameOver = "GAME OVER",
    restart = "RESTART",
    newBest = "NEW BEST!",
    settings = "SETTINGS",
    sound = "Sound",
    vibration = "Vibration",
    language = "Language",
    close = "CLOSE",
    perfect = "PERFECT!"
)

val IT = Strings(
    tapToStart = "TOCCA PER INIZIARE",
    tip = "Tocca per posare il blocco",
    floors = "PIANI",
    best = "RECORD",
    gameOver = "PARTITA FINITA",
    restart = "RICOMINCIA",
    newBest = "NUOVO RECORD!",
    settings = "OPZIONI",
    sound = "Suono",
    vibration = "Vibrazione",
    language = "Lingua",
    close = "CHIUDI",
    perfect = "PERFETTO!"
)

fun stringsFor(lang: Lang): Strings = if (lang == Lang.IT) IT else EN
