package com.thilux.kblog

import com.google.gson.Gson
import com.thilux.kblog.dto.CommentDto
import com.thilux.kblog.dto.DomainDto
import com.thilux.kblog.dto.PostDto
import com.thilux.kblog.repository.CommentRepository
import com.thilux.kblog.repository.PostRepository
import io.ktor.application.Application
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import org.jetbrains.squash.definition.TableDefinition
import org.junit.After
import org.junit.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.fail

/**
 * Created by tsantana on 16/12/17.
 */

class KBlogTest {


    private val json = "application/json"
    private val gson = Gson()
    private val postContent = gson.toJson(PostDto("test post", "post from test", LocalDateTime.now()))


    @After
    fun clear() {
        clearCommentsRepository()
        clearPostRepository()
    }

    private fun clearPostRepository(){
        PostRepository.getAll().forEach {
            PostRepository.remove(it.id!!)
        }

    }

    private fun clearCommentsRepository(){
        CommentRepository.getAll().forEach {
            CommentRepository.remove(it.id!!)
        }
    }

    @Test
    fun getAllPostsTest() = withTestApplication(Application::main){
        val post1 = savePost(gson.toJson(PostDto("post1", "bla bla bla", LocalDateTime.now())))
        val post2 = savePost(gson.toJson(PostDto("post2", "bla bla bla", LocalDateTime.now())))

        handleRequest(HttpMethod.Get, POSTS_ENDPOINT) {
            addHeader("Accept", json)
        }.response.let {
            assertEquals(HttpStatusCode.OK, it.status())
            val response = gson.fromJson(it.content, Array<PostDto>::class.java)
            response.find { it.title == post1.title } ?: fail()
            response.find { it.title == post2.title } ?: fail()
        }
        assertEquals(2, PostRepository.getAll().size)
    }

    @Test
    fun getPostTest() = withTestApplication(Application::main) {
        val post = savePost()
        handleRequest(HttpMethod.Get, "$POST_ENDPOINT/${post.id}") {
            addHeader("Accept", json)
        }.response.let {
            assertEquals(HttpStatusCode.OK, it.status())
        }

        assertEquals(1, PostRepository.getAll().size)
    }

    @Test
    fun getNonExistentPostTest() = withTestApplication(Application::main) {
        handleRequest(HttpMethod.Get, "$POST_ENDPOINT/1000") {
            addHeader("Accept", json)
        }.response.let {
            assertEquals(HttpStatusCode.InternalServerError, it.status())
        }
    }

    @Test
    fun deletePostTest() = withTestApplication(Application::main) {
        val post = savePost()
        handleRequest(HttpMethod.Delete, "$POST_ENDPOINT/${post.id}") {
            addHeader("Accept", json)
        }.response.let {
            assertEquals(HttpStatusCode.OK, it.status())
        }

        assertEquals(0, PostRepository.getAll().size)
    }

    @Test
    fun deleteNonExistentPostTest() = withTestApplication(Application::main) {
        handleRequest(HttpMethod.Delete, "$POST_ENDPOINT/1290") {
            addHeader("Accept", json)
        }.response.let {
            assertEquals(HttpStatusCode.InternalServerError, it.status())
        }
    }

    @Test
    fun getAllCommentsTest() = withTestApplication(Application::main) {
        val post = savePost()
        val comment1 = saveComment(gson.toJson(CommentDto(post.id!!, "testu", "comment1", LocalDateTime.now())))
        val comment2 = saveComment(gson.toJson(CommentDto(post.id!!, "testu", "comment2", LocalDateTime.now())))

        handleRequest(HttpMethod.Get, COMMENTS_ENDPOINT) {
            addHeader("Accept", json)
        }.response.let {
            assertEquals(HttpStatusCode.OK, it.status())
            val response = gson.fromJson(it.content, Array<CommentDto>::class.java)
            response.find { it.content == comment1.content }
            response.find { it.content == comment2.content }
        }

        assertEquals(2, CommentRepository.getAll().size)
    }

    @Test
    fun getCommentTest() = withTestApplication(Application::main) {
        val post = savePost()
        val comment = saveComment(null, post.id!!)
        handleRequest(HttpMethod.Get, "$COMMENT_ENDPOINT/${comment.id}") {
            addHeader("Accept", json)
        }.response.let {
            assertEquals(HttpStatusCode.OK, it.status())

        }

        assertEquals(1, CommentRepository.getAll().size)
    }

    @Test
    fun getNonExistentCommentTest() = withTestApplication(Application::main) {
        handleRequest(HttpMethod.Get, "$COMMENT_ENDPOINT/1219") {
            addHeader("Accept", json)
        }.response.let {
            assertEquals(HttpStatusCode.InternalServerError, it.status())
        }
    }

    @Test
    fun getAllCommentsForPostId() = withTestApplication(Application::main) {
        val post1 = savePost()
        val post2 = savePost()
        val comment1 = saveComment(gson.toJson(CommentDto(post1.id!!, "testu", "comment1", LocalDateTime.now())))
        saveComment(gson.toJson(CommentDto(post2.id!!, "testu", "comment2", LocalDateTime.now())))
        val comment3 = saveComment(gson.toJson(CommentDto(post1.id!!, "testu", "comment3", LocalDateTime.now())))
        handleRequest(HttpMethod.Get, "$POST_ENDPOINT/${post1.id!!}/comments") {
            addHeader("Accept", json)
        }.response.let {
            assertEquals(HttpStatusCode.OK, it.status())
            val response = gson.fromJson(it.content, Array<CommentDto>::class.java)
            response.find { it.content == comment1.content }
            response.find { it.content == comment3.content }

            assertEquals(2, response.size)
        }

        assertEquals(3, CommentRepository.getAll().size)

    }

    @Test
    fun deleteCommentTest() = withTestApplication(Application::main) {
        println("There are ${CommentRepository.getAll().size} Comments in the database")
        val post = savePost()
        val comment = saveComment(null, post.id!!)
        handleRequest(HttpMethod.Delete, "$COMMENT_ENDPOINT/${comment.id}") {
            addHeader("Accept", json)
        }.response.let {
            assertEquals(HttpStatusCode.OK, it.status())
        }

        assertEquals(0, CommentRepository.getAll().size)
    }

    @Test
    fun deleteNonExistentCommentTest() = withTestApplication(Application::main) {
        handleRequest(HttpMethod.Delete, "$COMMENT_ENDPOINT/7923") {
            addHeader("Accept", json)
        }.response.let {
            assertEquals(HttpStatusCode.InternalServerError, it.status())
        }
    }

    private fun <S: TableDefinition, T: DomainDto<S>> TestApplicationEngine.saveEntity(content: String, url: String, clazz: Class<T>): T {

        val postRequest = handleRequest(HttpMethod.Post, url) {
            body = content
            addHeader("Content-Type", json)
            addHeader("Accept", json)
        }

        with(postRequest) {
            assertEquals(HttpStatusCode.OK, response.status())
            return gson.fromJson(response.content,  clazz)
        }

    }

    private fun TestApplicationEngine.saveComment(commentJson: String?, postId: Int = 1): CommentDto {
        val comment = commentJson ?: gson.toJson(CommentDto(postId, "commentator1", "Bla bla bla", LocalDateTime.now()))
        println("CommentContent=> $comment")
        return saveEntity(comment, COMMENT_ENDPOINT, CommentDto::class.java)
    }

    private fun TestApplicationEngine.savePost(post: String = postContent): PostDto {
        println("PostContent=> $post")
        return saveEntity(post, POST_ENDPOINT, PostDto::class.java)
    }


}