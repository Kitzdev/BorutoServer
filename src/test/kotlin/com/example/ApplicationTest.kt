package com.example

import com.example.models.ApiResponse
import com.example.repository.HeroRepository
import com.example.repository.NEXT_PAGE_KEY
import com.example.repository.PREVIOUS_PAGE_KEY
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.koin.java.KoinJavaComponent.inject
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
    private val heroRepository: HeroRepository by inject(HeroRepository::class.java)

    @Test
    fun `access root endpoint, assert correct information`() {
        withTestApplication(moduleFunction = Application::module) {
            handleRequest(HttpMethod.Get, "/").apply {
                assertEquals(
                    expected = HttpStatusCode.OK,
                    actual = response.status(),
                )
                assertEquals(
                    expected = "Welcome to Boruto API!",
                    actual = response.content,
                )
            }
        }
    }

    private fun calculatePage(page: Int): Map<String, Int?> {
        var previousPage: Int? = page
        var nextPage: Int? = page
        if (page in 1..4) {
            nextPage = nextPage?.plus(1)
        }
        if (page in 2..5) {
            previousPage = previousPage?.minus(1)
        }
        if (page == 1) {
            previousPage = null
        }
        if (page == 5) {
            nextPage = null
        }

        return mapOf(PREVIOUS_PAGE_KEY to previousPage, NEXT_PAGE_KEY to nextPage)
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `access all heroes endpoint, query all pages, assert correct information`() {
        withTestApplication(moduleFunction = Application::module) {
            val pages = 1..5
            val heroes = listOf(
                heroRepository.page1,
                heroRepository.page2,
                heroRepository.page3,
                heroRepository.page4,
                heroRepository.page5,
            )
            pages.forEach { page ->
                handleRequest(HttpMethod.Get, "/boruto/heroes?page=$page").apply {
                    println("CURRENT PAGE: $page")
                    assertEquals(
                        expected = HttpStatusCode.OK,
                        actual = response.status(),
                    )
                    val expected = ApiResponse(
                        success = true,
                        message = "ok",
                        previousPage = calculatePage(page = page)["previousPage"],
                        nextPage = calculatePage(page = page)["nextPage"],
                        heroes = heroes[page - 1],
                    )
                    val actual = Json.decodeFromString<ApiResponse>(response.content.toString())
                    println("PREVIOUS PAGE: ${calculatePage(page = page)["previousPage"]}")
                    println("NEXT PAGE: ${calculatePage(page = page)["nextPage"]}")
                    println("HEROES: ${heroes[page - 1]}")
                    assertEquals(
                        expected = expected,
                        actual = actual,
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `access all heroes endpoint, query invalid page number, assert error`() {
        withTestApplication(moduleFunction = Application::module) {
            handleRequest(HttpMethod.Get, "/boruto/heroes?page=invalid").apply {
                assertEquals(
                    expected = HttpStatusCode.BadRequest,
                    actual = response.status(),
                )
                val expected = ApiResponse(
                    success = false,
                    message = "Only Numbers Allowed.",
                )
                val actual = Json.decodeFromString<ApiResponse>(response.content.toString())
                println("EXPECTED: $expected")
                println("actual: $actual")
                assertEquals(
                    expected = expected,
                    actual = actual,
                )
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `access all heroes endpoint, query non existing page number, assert error`() {
        withTestApplication(moduleFunction = Application::module) {
            handleRequest(HttpMethod.Get, "/boruto/heroes?page=6").apply {
                assertEquals(
                    expected = HttpStatusCode.NotFound,
                    actual = response.status(),
                )
                val expected = ApiResponse(
                    success = false,
                    message = "Heroes not found.",
                )
                val actual = Json.decodeFromString<ApiResponse>(response.content.toString())
                println("EXPECTED: $expected")
                println("actual: $actual")
                assertEquals(
                    expected = expected,
                    actual = actual,
                )
            }
        }
    }

}