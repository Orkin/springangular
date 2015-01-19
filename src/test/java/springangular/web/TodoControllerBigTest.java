package springangular.web;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import springangular.domain.Todo;
import springangular.repository.TodoRepository;
import springangular.web.dto.TodoDTO;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpStatus.*;
import static springangular.web.exception.ErrorCode.NO_ENTITY_DELETION;
import static springangular.web.exception.ErrorCode.WRONG_ENTITY_INFORMATION;

/**
 * Big tests (or blackbox test / End to End)
 * Pushes an API call, checks that DB has expected datas
 * 
 **/
public class TodoControllerBigTest extends WebAppTest{

    @Autowired
    private TodoRepository todoRepository;

    private Todo savedTodo;
    
    @Before
    public void setUp() {
        savedTodo = new Todo.Builder().withTitle("Test").withDescription("Description Test").build();
        Todo secondTodoTest = new Todo.Builder().withTitle("Secondtest").withDescription("Second description test").build();

        todoRepository.save(savedTodo);
        todoRepository.save(secondTodoTest);
    }
    
    @After
    public void tearDown() {
        todoRepository.deleteAll();
    }

    @Test
    public void should_Get_AllTodos_WithTwoTestTodoInResult() {
        given()
            .log().all()
        .when()
            .get("/todo")
        .then()
            .log().all()
            .statusCode(OK.value())
            .body("[0].id", is(savedTodo.getId()
                                        .intValue()))
            .body("[0].title", is(savedTodo.getTitle()))
            .body("[0].description", is(savedTodo.getDescription()));
    }

    @Test
    public void should_Get_OneTodoById_WithOneTestTodoInResult() {
        given()
            .log().all()
        .when()
            .get("/todo/{id}", savedTodo.getId())
        .then()
            .log().all()
            .statusCode(OK.value())
            .body("todo.id", is(savedTodo.getId()
                                         .intValue()))
            .body("todo.title", is(savedTodo.getTitle()))
            .body("todo.description", is(savedTodo.getDescription()));
    }
    
    @Test
    public void should_Create_OneTodo_Nominal() {
        final String todoTitle = "NewTest";
        final String todoDescription = "NewDesc";
        Todo todoToCreate = new Todo.Builder().withTitle(todoTitle).withDescription(todoDescription).build();
        TodoDTO todoDTO = new TodoDTO(todoToCreate);

        given()
            .header("Content-Type", "application/json")
            .body(todoDTO)
            .log().all()
        .when()
            .put("/todo")
        .then()
            .log().all()
            .statusCode(OK.value())
            .body("todo.id", notNullValue())
            .body("todo.title", is(todoTitle))
            .body("todo.description", is(todoDescription));

        // And then assert what has been done in db
        Todo createdTodo = todoRepository.findByTitle(todoTitle);

        assertThat(createdTodo, notNullValue());
        assertThat(createdTodo.getId(), notNullValue());
        assertThat(createdTodo.getDescription(), is(todoDescription));
    }
    
    @Test
    public void shouldNot_Create_Todo_WhenNoTitle() {
        TodoDTO todoDTO = new TodoDTO(new Todo.Builder().withDescription("Description").build());
        
        given()
            .header("Content-Type", "application/json")
            .body(todoDTO)
            .log().all()
        .when()
            .put("/todo")
        .then()
            .statusCode(BAD_REQUEST.value())
            .log().all()
            .body("url", is("/todo"))
            .body("errorCode", is(WRONG_ENTITY_INFORMATION.getCode()))
            .body("reasonCause", is(WRONG_ENTITY_INFORMATION.getDescription()));

    }
    
    @Test
    public void should_Update_Todo_Nominal() {
        final String updatedTitle = "NewTitle of todo";
        final String updatedDescription = "NewDescription of todo";
        savedTodo.setTitle(updatedTitle);
        savedTodo.setDescription(updatedDescription);

        TodoDTO todoDTO = new TodoDTO(savedTodo);

        given()
            .header("Content-Type", "application/json")
            .body(todoDTO)
            .log().all()
        .when()
            .put("/todo")
        .then()
            .log().all()
            .statusCode(OK.value())
            .body("todo.id", is(savedTodo.getId()
                                         .intValue()))
            .body("todo.title", is(updatedTitle))
            .body("todo.description", is(updatedDescription));

        Todo updatedTodo = todoRepository.findByTitle(updatedTitle);
        
        assertThat(updatedTodo, notNullValue());
        assertThat(updatedTodo.getId(), is(savedTodo.getId()));
        assertThat(updatedTodo.getDescription(), is(updatedDescription));
    }
    
    @Test
    public void should_Delete_Todo_Nominal() {
        long initialTotalEntries = todoRepository.count();

        // Start with api test
        given()
            .log().all()
        .when()
            .delete("/todo/{id}", savedTodo.getId())
        .then()
            .log().all()
            .statusCode(NO_CONTENT.value());

        // Recheck todos db count and verify if it has well changed by -1
        long finalTotalEntries = todoRepository.count();
        assertThat(finalTotalEntries, not(initialTotalEntries));
        assertThat(finalTotalEntries, is(initialTotalEntries - 1));
    }
    
    @Test
    public void shouldNot_Delete_Todo_WhenNotFound() {
        long initialTotalEntries = todoRepository.count();

        given()
            .log().all()
        .when()
            .delete("/todo/{id}", 100)
        .then()
            .log().all()
            .statusCode(NOT_FOUND.value())
            .body("url", is("/todo/100"))
            .body("errorCode", is(NO_ENTITY_DELETION.getCode()))
            .body("reasonCause", is(NO_ENTITY_DELETION.getDescription()));

        long finalTotalEntries = todoRepository.count();
        assertThat(finalTotalEntries, is(initialTotalEntries));
    }
}