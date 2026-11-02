package com.nihad64.rotam.domain.usecase

import com.nihad64.rotam.data.model.Plan
import com.nihad64.rotam.data.repository.PlanRepository
import com.nihad64.rotam.notification.ReminderScheduler
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class GetPlansForDay(private val repo: PlanRepository) {
    operator fun invoke(date: LocalDate) = repo.observeDay(date)
}

class CreatePlan(private val repo: PlanRepository, private val scheduler: ReminderScheduler) {
    suspend operator fun invoke(plan: Plan): List<Plan> {
        val created = repo.create(plan)
        created.forEach { scheduler.schedule(it) }
        return created
    }
}

class CompletePlan(private val repo: PlanRepository, private val scheduler: ReminderScheduler) {
    suspend operator fun invoke(plan: Plan, completed: Boolean = !plan.isCompleted) {
        repo.update(plan.copy(isCompleted = completed, isSkipped = false))
        if (completed) scheduler.cancel(plan.id) else scheduler.schedule(plan.copy(isCompleted = completed, isSkipped = false))
    }
}

class SkipPlan(private val repo: PlanRepository, private val scheduler: ReminderScheduler) {
    suspend operator fun invoke(plan: Plan) {
        repo.update(plan.copy(isSkipped = true, isCompleted = false))
        scheduler.cancel(plan.id)
    }
}

class DeletePlan(private val repo: PlanRepository, private val scheduler: ReminderScheduler) {
    suspend operator fun invoke(plan: Plan) {
        scheduler.cancel(plan.id)
        repo.delete(plan)
    }
}

/** Kaçırılan planı yarına (aynı saatte) taşır. */
class MovePlanToTomorrow(private val repo: PlanRepository, private val scheduler: ReminderScheduler) {
    suspend operator fun invoke(plan: Plan) {
        val moved = PlanTime.moved(plan, LocalDate.now().plusDays(1)).copy(isSkipped=false,isCompleted=false)
        repo.update(moved)
        scheduler.schedule(moved)
    }
}

