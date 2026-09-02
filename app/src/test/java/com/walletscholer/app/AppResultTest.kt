package com.walletscholer.app

import com.walletscholer.app.data.model.DefaultCategories
import com.walletscholer.app.domain.result.AppResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppResultTest {

    @Test
    fun testSuccessResult() {
        val success: AppResult<String> = AppResult.Success("Hello")
        assertTrue(success.isSuccess)
        assertFalse(success.isError)
        assertEquals("Hello", success.getOrNull())
        assertEquals("Hello", success.getOrDefault("Default"))

        var callbackTriggered = false
        success.onSuccess { data ->
            assertEquals("Hello", data)
            callbackTriggered = true
        }
        assertTrue(callbackTriggered)

        val mapped = success.map { it.length }
        assertTrue(mapped.isSuccess)
        assertEquals(5, mapped.getOrNull())
    }

    @Test
    fun testErrorResult() {
        val error: AppResult<String> = AppResult.Error("Network failure", RuntimeException("Timeout"), 500)
        assertFalse(error.isSuccess)
        assertTrue(error.isError)
        assertNull(error.getOrNull())
        assertEquals("Default", error.getOrDefault("Default"))

        var errorCallbackTriggered = false
        error.onError { msg, _ ->
            assertEquals("Network failure", msg)
            errorCallbackTriggered = true
        }
        assertTrue(errorCallbackTriggered)
    }

    @Test
    fun testValidationErrorResult() {
        val validation: AppResult<String> = AppResult.ValidationError("amount", "Must be positive")
        assertFalse(validation.isSuccess)
        assertTrue(validation.isError)
        assertNull(validation.getOrNull())

        var validationTriggered = false
        validation.onError { msg, _ ->
            assertTrue(msg.contains("amount"))
            assertTrue(msg.contains("Must be positive"))
            validationTriggered = true
        }
        assertTrue(validationTriggered)
    }
}

class DefaultCategoriesTest {

    @Test
    fun testDefaultCategoriesLookup() {
        val rent = DefaultCategories.findCategory("rent")
        assertEquals("Rent", rent.name)
        assertEquals("NEEDS", rent.group)

        val food = DefaultCategories.findCategory("food")
        assertEquals("Food", food.name)
        assertEquals("NEEDS", food.group)

        val salary = DefaultCategories.findCategory("salary")
        assertEquals("Salary", salary.name)
        assertEquals("INCOME", salary.group)

        val fallback = DefaultCategories.findCategory("custom_item")
        assertEquals("Custom_item", fallback.name)
    }

    @Test
    fun testGroupings() {
        val needs = DefaultCategories.EXPENSE_CATEGORIES.filter { it.group == "NEEDS" }
        assertTrue(needs.isNotEmpty())
        assertTrue(needs.any { it.id == "rent" })
        assertTrue(needs.any { it.id == "food" })
        assertTrue(needs.any { it.id == "transport" })

        val wants = DefaultCategories.EXPENSE_CATEGORIES.filter { it.group == "WANTS" }
        assertTrue(wants.isNotEmpty())
        assertTrue(wants.any { it.id == "shopping" })
        assertTrue(wants.any { it.id == "entertainment" })

        val savings = DefaultCategories.EXPENSE_CATEGORIES.filter { it.group == "SAVINGS" }
        assertTrue(savings.isNotEmpty())
        assertTrue(savings.any { it.id == "savings" })
        assertTrue(savings.any { it.id == "investment" })
    }
}
