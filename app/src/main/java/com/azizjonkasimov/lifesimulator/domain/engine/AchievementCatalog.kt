package com.azizjonkasimov.lifesimulator.domain.engine

import com.azizjonkasimov.lifesimulator.domain.model.AssetKind
import com.azizjonkasimov.lifesimulator.domain.model.EducationLevel
import com.azizjonkasimov.lifesimulator.domain.model.GameState
import com.azizjonkasimov.lifesimulator.domain.model.RelationType

/**
 * One-time milestones unlocked when their [predicate] first holds. They're checked
 * after every year and action; a newly-earned one is logged and surfaced on the
 * Profile and Legacy screens. Bragging rights, not mechanics.
 */
object AchievementCatalog {
    data class Achievement(
        val id: String,
        val name: String,
        val description: String,
        val predicate: (GameState) -> Boolean,
    )

    private fun children(state: GameState) =
        state.relationships.count { it.alive && it.relation == RelationType.CHILD }

    val all: List<Achievement> = listOf(
        Achievement("scholar", "Scholar", "Max out your Smarts.") { it.character.stats.smarts >= 100 },
        Achievement("heartthrob", "Heartthrob", "Max out your Looks.") { it.character.stats.looks >= 100 },
        Achievement("blissful", "Blissful", "Max out your Happiness.") { it.character.stats.happiness >= 100 },
        Achievement("graduate", "Higher Learning", "Earn a graduate degree.") { it.education.level == EducationLevel.GRADUATE },
        Achievement("wealthy", "Well-off", "Reach a net worth of $100,000.") { it.netWorth >= 100_000 },
        Achievement("millionaire", "Millionaire", "Reach a net worth of $1,000,000.") { it.netWorth >= 1_000_000 },
        Achievement("homeowner", "Homeowner", "Own a place of your own.") { st -> st.assets.any { it.kind == AssetKind.PROPERTY } },
        Achievement("high_roller", "High Roller", "Own a luxury item.") { st -> st.assets.any { it.kind == AssetKind.LUXURY } },
        Achievement("married", "Newlywed", "Get married.") { st -> st.relationships.any { it.alive && it.relation == RelationType.SPOUSE } },
        Achievement("big_family", "Full House", "Raise four or more children.") { children(it) >= 4 },
        Achievement("top_of_career", "Top of the Ladder", "Reach the top rung of a career.") { st ->
            st.job?.let { JobCatalog.promoted(it) == null } ?: false
        },
        Achievement("jailbird", "Jailbird", "Do time behind bars.") { it.inPrison || "ex_convict" in it.flags },
        Achievement("survivor", "Golden Years", "Live to 65.") { it.age >= 65 },
        Achievement("centenarian", "Centenarian", "Live to 100.") { it.age >= 100 },
    )
}
