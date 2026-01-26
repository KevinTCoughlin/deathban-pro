package dev.coughlin.deathban.data

import java.time.Instant
import java.util.UUID

data class SharedLivesPool(
    val id: String,
    var lives: Int,
    var maxLives: Int,
    val members: MutableSet<UUID> = mutableSetOf(),
    val contributions: MutableMap<UUID, Int> = mutableMapOf(),
    var lastModified: Instant = Instant.now()
) {
    companion object {
        const val GLOBAL_POOL_ID = "global"
    }

    fun addLife(contributor: UUID? = null): Boolean {
        if (lives >= maxLives) return false
        lives++
        contributor?.let {
            contributions[it] = contributions.getOrDefault(it, 0) + 1
        }
        lastModified = Instant.now()
        return true
    }

    fun removeLife(): Boolean {
        if (lives <= 0) return false
        lives--
        lastModified = Instant.now()
        return true
    }

    fun addMember(uuid: UUID): Boolean {
        lastModified = Instant.now()
        return members.add(uuid)
    }

    fun removeMember(uuid: UUID): Boolean {
        lastModified = Instant.now()
        return members.remove(uuid)
    }

    fun isMember(uuid: UUID): Boolean = members.contains(uuid)

    fun getContribution(uuid: UUID): Int = contributions.getOrDefault(uuid, 0)

    fun isEmpty(): Boolean = lives <= 0

    fun isFull(): Boolean = lives >= maxLives
}
