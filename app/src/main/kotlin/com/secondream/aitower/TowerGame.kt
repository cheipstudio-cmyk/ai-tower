package com.secondream.aitower

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

enum class TState { READY, PLAYING, OVER }

enum class TapResult { NONE, STARTED, PLACED, PERFECT, GAMEOVER }

class Block(var x: Float, var width: Float, val level: Int)

class Debris(
    var x: Float,
    var worldY: Float,
    var width: Float,
    var vx: Float,
    var vy: Float,
    var rot: Float = 0f,
    var vr: Float = 0f
)

class Spark(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float,
    val maxLife: Float,
    val size: Float
)

class TowerGame {
    val blocks = ArrayList<Block>()
    var moving: Block = Block(0f, 0f, 1)
    var dir = 1f
    var speed = 300f
    var state = TState.READY
    var score = 0
    var combo = 0
    var best = 0
    var justBeatBest = false
    var perfectFlash = 0f

    var camY = 0f
    val debris = ArrayList<Debris>()
    val sparks = ArrayList<Spark>()

    var screenW = 0f
    var screenH = 0f
    var blockH = 110f
    var dropLineY = 0f

    private val margin get() = screenW * 0.06f
    private val baseWidth get() = screenW * 0.56f
    private val maxSpeed = 760f
    private val perfectTol get() = screenW * 0.012f

    fun configure(w: Float, h: Float) {
        val first = screenW == 0f
        screenW = w
        screenH = h
        blockH = w * 0.10f
        dropLineY = h * 0.32f
        if (first) reset()
    }

    fun reset() {
        blocks.clear()
        debris.clear()
        sparks.clear()
        score = 0
        combo = 0
        justBeatBest = false
        perfectFlash = 0f
        speed = 300f
        dir = 1f
        val bw = baseWidth
        blocks.add(Block((screenW - bw) / 2f, bw, 0))
        moving = Block(margin, bw, 1)
        camY = camTargetFor()
        state = TState.READY
    }

    fun restart() {
        reset()
        state = TState.PLAYING
    }

    private fun camTargetFor(): Float = -moving.level * blockH - dropLineY

    fun onTap(): TapResult {
        return when (state) {
            TState.READY -> {
                state = TState.PLAYING
                TapResult.STARTED
            }
            TState.PLAYING -> drop()
            TState.OVER -> TapResult.NONE
        }
    }

    private fun drop(): TapResult {
        val prev = blocks.last()
        val left = max(moving.x, prev.x)
        val right = min(moving.x + moving.width, prev.x + prev.width)
        val overlap = right - left
        if (overlap <= 0f) {
            gameOver()
            return TapResult.GAMEOVER
        }

        val perfect = abs(moving.x - prev.x) <= perfectTol
        val placedX: Float
        val placedW: Float
        if (perfect) {
            placedX = prev.x
            placedW = min(prev.width + 6f, baseWidth)
            combo++
            perfectFlash = 1f
            spawnSparks(prev.x + prev.width / 2f, dropLineY + blockH / 2f)
        } else {
            combo = 0
            placedX = left
            placedW = overlap
            if (moving.x < prev.x) {
                spawnDebris(moving.x, prev.x - moving.x)
            } else {
                val overhangLeft = prev.x + prev.width
                spawnDebris(overhangLeft, (moving.x + moving.width) - overhangLeft)
            }
        }

        blocks.add(Block(placedX, placedW, moving.level))
        score = blocks.size - 1

        val nextLevel = moving.level + 1
        val startLeft = if (dir > 0f) margin else screenW - margin - placedW
        moving = Block(startLeft, placedW, nextLevel)
        dir = -dir
        speed = min(speed + 7f, maxSpeed)
        return if (perfect) TapResult.PERFECT else TapResult.PLACED
    }

    private fun spawnDebris(x: Float, w: Float) {
        if (w <= 0f) return
        debris.add(
            Debris(
                x = x,
                worldY = -moving.level * blockH,
                width = w,
                vx = if (x < screenW / 2f) -40f else 40f,
                vy = -30f,
                vr = (Math.random().toFloat() - 0.5f) * 6f
            )
        )
    }

    private fun spawnSparks(cx: Float, cy: Float) {
        repeat(14) { i ->
            val ang = (i / 14f) * 6.2832f
            val sp = 120f + Math.random().toFloat() * 160f
            sparks.add(
                Spark(
                    x = cx,
                    y = cy,
                    vx = cos(ang) * sp,
                    vy = sin(ang) * sp,
                    life = 0.5f + Math.random().toFloat() * 0.3f,
                    maxLife = 0.8f,
                    size = 5f + Math.random().toFloat() * 4f
                )
            )
        }
    }

    private fun gameOver() {
        state = TState.OVER
        justBeatBest = score > best
        if (justBeatBest) best = score
    }

    fun update(dtMs: Float) {
        val dt = dtMs / 1000f
        if (state == TState.PLAYING) {
            moving.x += dir * speed * dt
            val maxX = screenW - margin - moving.width
            val minX = margin
            if (moving.x > maxX) {
                moving.x = maxX
                dir = -1f
            }
            if (moving.x < minX) {
                moving.x = minX
                dir = 1f
            }
        }

        val target = camTargetFor()
        camY += (target - camY) * min(1f, dt * 9f)

        val di = debris.iterator()
        while (di.hasNext()) {
            val d = di.next()
            d.x += d.vx * dt
            d.worldY += d.vy * dt
            d.vy += 1400f * dt
            d.rot += d.vr * dt
            if (d.worldY - camY > screenH + 200f) di.remove()
        }

        val si = sparks.iterator()
        while (si.hasNext()) {
            val s = si.next()
            s.x += s.vx * dt
            s.y += s.vy * dt
            s.vy += 500f * dt
            s.life -= dt
            if (s.life <= 0f) si.remove()
        }

        if (perfectFlash > 0f) perfectFlash = max(0f, perfectFlash - dt * 1.5f)
    }
}
