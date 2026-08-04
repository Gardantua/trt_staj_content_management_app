package com.trt.contentengagement.gameplay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.trt.contentengagement.identity.infrastructure.security.TemporaryHeaderAuthenticationFilter;
import com.trt.contentengagement.messaging.application.QuizCompletedXpEventHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GameplayIntegrationTest {
    private static final UUID USER_ID=UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_USER_ID=UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID EDITOR_ID=UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ADMIN_ID=UUID.fromString("44444444-4444-4444-4444-444444444444");
    @Container @ServiceConnection static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:17.5-alpine"))
                    .withDatabaseName("gameplay_test").withUsername("content_engagement")
                    .withPassword("test_password");
    private final JdbcTemplate jdbc; private final ObjectMapper mapper;
    private final QuizCompletedXpEventHandler xpEventHandler;
    private final HttpClient client=HttpClient.newHttpClient();
    @LocalServerPort private int port;
    @Autowired GameplayIntegrationTest(
            JdbcTemplate jdbc,
            ObjectMapper mapper,
            QuizCompletedXpEventHandler xpEventHandler
    ) {
        this.jdbc=jdbc;
        this.mapper=mapper;
        this.xpEventHandler=xpEventHandler;
    }

    @BeforeEach void clear(){
        jdbc.update("DELETE FROM inbox_messages");
        jdbc.update("DELETE FROM outbox_events");
        jdbc.update("DELETE FROM xp_transactions");
        jdbc.update("DELETE FROM gameplay_answers"); jdbc.update("DELETE FROM gameplay_attempts");
        jdbc.update("DELETE FROM admin_audit_entries"); jdbc.update("DELETE FROM quiz_answer_options");
        jdbc.update("DELETE FROM quiz_questions"); jdbc.update("DELETE FROM quiz_versions");
        jdbc.update("DELETE FROM quiz_definitions"); jdbc.update("DELETE FROM catalog_episodes");
        jdbc.update("DELETE FROM catalog_seasons"); jdbc.update("DELETE FROM catalog_contents");
        jdbc.update("DELETE FROM media_assets");
    }

    @Test void answerIsLockedThenCorrectOptionIsRevealedBeforeNextQuestion() throws Exception {
        QuizFixture quiz=publishedQuiz();
        HttpResponse<String> start=send("POST","/api/v1/quizzes/"+quiz.quizId()+"/attempts",null,USER_ID,"USER",null);
        assertThat(start.statusCode()).isEqualTo(200);
        assertThat(start.body()).doesNotContain("correctOptionId").doesNotContain("\"correct\"");
        JsonNode startJson=json(start); String attemptId=startJson.get("attemptId").stringValue();
        assertThat(startJson.get("currentQuestion").get("questionId").stringValue()).isEqualTo(quiz.q1());
        assertThat(startJson.get("currentQuestion").get("visual").get("role").stringValue())
                .isEqualTo("DECORATIVE");
        assertThat(startJson.get("currentQuestion").get("visual").get("contentUrl").stringValue())
                .startsWith("/api/v1/media/");

        HttpResponse<String> first=answer(attemptId,quiz.q1(),quiz.q1Correct(),"key-1",USER_ID);
        assertThat(first.statusCode()).isEqualTo(200);
        assertThat(first.body()).contains("\"correct\":true")
                .contains("\"correctOptionId\":\""+quiz.q1Correct()+"\"")
                .contains("\"questionId\":\""+quiz.q2()+"\"");

        HttpResponse<String> second=answer(attemptId,quiz.q2(),quiz.q2Wrong(),"key-2",USER_ID);
        assertThat(second.statusCode()).isEqualTo(200);
        assertThat(second.body()).contains("\"correct\":false")
                .contains("\"attemptStatus\":\"COMPLETED\"")
                .contains("\"score\":100")
                .contains("\"earnedXp\":100");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM gameplay_answers",Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM outbox_events",Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM xp_transactions",Integer.class)).isZero();
        consumeCompletionEvent(attemptId);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM xp_transactions",Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT amount FROM xp_transactions",Integer.class)).isEqualTo(100);

        HttpResponse<String> repeated=answer(attemptId,quiz.q2(),quiz.q2Wrong(),"key-2",USER_ID);
        assertThat(repeated.statusCode()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM outbox_events",Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM xp_transactions",Integer.class)).isEqualTo(1);

        HttpResponse<String> summary=send("GET","/api/v1/me/xp",null,USER_ID,"USER",null);
        assertThat(summary.statusCode()).isEqualTo(200);
        assertThat(summary.body()).contains("\"totalXp\":100").contains("\"transactionCount\":1");
    }

    @Test void ownerBoundaryAndIdempotencyPayloadConflictAreEnforced() throws Exception {
        QuizFixture quiz=publishedQuiz(); String attemptId=json(send(
                "POST","/api/v1/quizzes/"+quiz.quizId()+"/attempts",null,USER_ID,"USER",null
        )).get("attemptId").stringValue();
        HttpResponse<String> first=answer(attemptId,quiz.q1(),quiz.q1Correct(),"same-key",USER_ID);
        HttpResponse<String> repeat=answer(attemptId,quiz.q1(),quiz.q1Correct(),"same-key",USER_ID);
        HttpResponse<String> conflict=answer(attemptId,quiz.q1(),quiz.q1Wrong(),"same-key",USER_ID);
        HttpResponse<String> other=send("GET","/api/v1/attempts/"+attemptId,null,OTHER_USER_ID,"USER",null);
        assertThat(first.body()).isEqualTo(repeat.body());
        assertThat(conflict.statusCode()).isEqualTo(409);
        assertThat(conflict.body()).contains("IDEMPOTENCY_KEY_CONFLICT");
        assertThat(other.statusCode()).isEqualTo(404);
        assertThat(other.body()).contains("ATTEMPT_NOT_FOUND");
    }

    @Test void parallelAnswersToSameQuestionProduceOnePersistentAnswer() throws Exception {
        QuizFixture quiz=publishedQuiz(); String attemptId=json(send(
                "POST","/api/v1/quizzes/"+quiz.quizId()+"/attempts",null,USER_ID,"USER",null
        )).get("attemptId").stringValue();
        CompletableFuture<HttpResponse<String>> one=CompletableFuture.supplyAsync(() -> uncheckedAnswer(
                attemptId,quiz.q1(),quiz.q1Correct(),"parallel-1"));
        CompletableFuture<HttpResponse<String>> two=CompletableFuture.supplyAsync(() -> uncheckedAnswer(
                attemptId,quiz.q1(),quiz.q1Wrong(),"parallel-2"));
        int firstStatus=one.join().statusCode(), secondStatus=two.join().statusCode();
        assertThat(java.util.List.of(firstStatus,secondStatus)).containsExactlyInAnyOrder(200,409);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM gameplay_answers",Integer.class)).isEqualTo(1);
    }

    @Test void extendedTimingPolicyCreatesTenTimesLongerServerDeadline() throws Exception {
        QuizFixture quiz=publishedQuiz();
        HttpResponse<String> response=send(
                "POST","/api/v1/quizzes/"+quiz.quizId()+"/attempts",
                "{\"timingPolicyVersion\":\"EXTENDED_V1\"}",USER_ID,"USER",null
        );

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode responseJson=json(response);
        assertThat(responseJson.get("timingPolicyVersion").stringValue()).isEqualTo("EXTENDED_V1");
        Instant startedAt=Instant.parse(responseJson.get("startedAt").stringValue());
        Instant deadline=Instant.parse(responseJson.get("deadline").stringValue());
        assertThat(Duration.between(startedAt,deadline)).isEqualTo(Duration.ofMinutes(50));
        assertThat(jdbc.queryForObject(
                "SELECT timing_policy_version FROM gameplay_attempts WHERE id = ?::uuid",
                String.class,responseJson.get("attemptId").stringValue()
        )).isEqualTo("EXTENDED_V1");
    }

    @Test void parallelRepeatedCompleteCreatesOneOutboxEventAndOneXpTransaction() throws Exception {
        QuizFixture quiz=publishedQuiz();
        String attemptId=json(send(
                "POST","/api/v1/quizzes/"+quiz.quizId()+"/attempts",null,USER_ID,"USER",null
        )).get("attemptId").stringValue();
        answer(attemptId,quiz.q1(),quiz.q1Correct(),"answer-1",USER_ID);
        answer(attemptId,quiz.q2(),quiz.q2Correct(),"answer-2",USER_ID);

        CompletableFuture<HttpResponse<String>> first=CompletableFuture.supplyAsync(() ->
                uncheckedSend("POST","/api/v1/attempts/"+attemptId+"/complete",null,USER_ID,"USER","complete-1"));
        CompletableFuture<HttpResponse<String>> second=CompletableFuture.supplyAsync(() ->
                uncheckedSend("POST","/api/v1/attempts/"+attemptId+"/complete",null,USER_ID,"USER","complete-2"));

        assertThat(java.util.List.of(first.join().statusCode(),second.join().statusCode()))
                .containsExactly(200,200);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM outbox_events WHERE aggregate_id = ?::uuid",
                Integer.class,attemptId
        )).isEqualTo(1);
        consumeCompletionEvent(attemptId);
        consumeCompletionEvent(attemptId);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM xp_transactions WHERE source_attempt_id = ?::uuid",
                Integer.class,attemptId
        )).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT amount FROM xp_transactions WHERE source_attempt_id = ?::uuid",
                Integer.class,attemptId
        )).isEqualTo(200);
    }

    @Test void adminCorrectionAppendsSignedTransactionWithoutChangingOriginal() throws Exception {
        QuizFixture quiz=publishedQuiz();
        String attemptId=json(send(
                "POST","/api/v1/quizzes/"+quiz.quizId()+"/attempts",null,USER_ID,"USER",null
        )).get("attemptId").stringValue();
        answer(attemptId,quiz.q1(),quiz.q1Correct(),"answer-1",USER_ID);
        answer(attemptId,quiz.q2(),quiz.q2Wrong(),"answer-2",USER_ID);
        consumeCompletionEvent(attemptId);
        String originalTransactionId=jdbc.queryForObject(
                "SELECT id::text FROM xp_transactions WHERE source_attempt_id = ?::uuid",
                String.class,attemptId
        );
        String adjustmentBody="{\"amount\":-40,\"referenceKey\":\"support-case-42\","
                + "\"note\":\"Verified score correction\"}";

        HttpResponse<String> forbidden=send(
                "POST","/api/v1/admin/xp-transactions/"+originalTransactionId+"/adjustments",
                adjustmentBody,EDITOR_ID,"EDITOR",null
        );
        HttpResponse<String> created=send(
                "POST","/api/v1/admin/xp-transactions/"+originalTransactionId+"/adjustments",
                adjustmentBody,ADMIN_ID,"ADMIN",null
        );
        HttpResponse<String> repeated=send(
                "POST","/api/v1/admin/xp-transactions/"+originalTransactionId+"/adjustments",
                adjustmentBody,ADMIN_ID,"ADMIN",null
        );
        HttpResponse<String> conflict=send(
                "POST","/api/v1/admin/xp-transactions/"+originalTransactionId+"/adjustments",
                "{\"amount\":-20,\"referenceKey\":\"support-case-42\",\"note\":\"Different\"}",
                ADMIN_ID,"ADMIN",null
        );

        assertThat(forbidden.statusCode()).isEqualTo(403);
        assertThat(created.statusCode()).isEqualTo(200);
        assertThat(repeated.statusCode()).isEqualTo(200);
        assertThat(json(created).get("id").stringValue()).isEqualTo(json(repeated).get("id").stringValue());
        assertThat(conflict.statusCode()).isEqualTo(409);
        assertThat(conflict.body()).contains("XP_REFERENCE_CONFLICT");
        assertThat(jdbc.queryForObject(
                "SELECT amount FROM xp_transactions WHERE id = ?::uuid",
                Integer.class,originalTransactionId
        )).isEqualTo(100);
        HttpResponse<String> summary=send("GET","/api/v1/me/xp",null,USER_ID,"USER",null);
        assertThat(summary.body()).contains("\"totalXp\":60").contains("\"transactionCount\":2");
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM admin_audit_entries WHERE action = 'XP_ADJUSTMENT_CREATED'",
                Integer.class
        )).isEqualTo(1);
    }

    @Test void consumerConflictDoesNotRollBackCompletedAttemptAndInboxClaimRollsBack() throws Exception {
        QuizFixture quiz=publishedQuiz();
        String attemptId=json(send(
                "POST","/api/v1/quizzes/"+quiz.quizId()+"/attempts",null,USER_ID,"USER",null
        )).get("attemptId").stringValue();
        answer(attemptId,quiz.q1(),quiz.q1Correct(),"answer-1",USER_ID);
        jdbc.update("""
                INSERT INTO xp_transactions
                    (id, user_id, content_id, amount, reason, policy_version, reference_key,
                     source_attempt_id, occurred_at)
                SELECT ?, ?, quiz.content_id, 999, 'QUIZ_COMPLETED', 'SCORE_MATCH_V1',
                       ?, ?::uuid, now()
                FROM gameplay_attempts AS attempt
                JOIN quiz_definitions AS quiz ON quiz.id = attempt.quiz_id
                WHERE attempt.id = ?::uuid
                """,UUID.randomUUID(),USER_ID,"conflicting-source:"+attemptId,
                attemptId,attemptId);

        HttpResponse<String> response=answer(
                attemptId,quiz.q2(),quiz.q2Wrong(),"answer-2",USER_ID
        );

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"attemptStatus\":\"COMPLETED\"");
        assertThatThrownBy(() -> consumeCompletionEvent(attemptId))
                .hasMessageContaining("different XP transaction");
        assertThat(jdbc.queryForObject(
                "SELECT status FROM gameplay_attempts WHERE id = ?::uuid",
                String.class,attemptId
        )).isEqualTo("COMPLETED");
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM gameplay_answers WHERE attempt_id = ?::uuid",
                Integer.class,attemptId
        )).isEqualTo(2);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM inbox_messages",
                Integer.class
        )).isZero();
    }

    @Test void outboxConflictRollsBackTheAnswerThatWouldCompleteAttempt() throws Exception {
        QuizFixture quiz=publishedQuiz();
        String attemptId=json(send(
                "POST","/api/v1/quizzes/"+quiz.quizId()+"/attempts",null,USER_ID,"USER",null
        )).get("attemptId").stringValue();
        answer(attemptId,quiz.q1(),quiz.q1Correct(),"answer-1",USER_ID);
        UUID outboxEventId=UUID.nameUUIDFromBytes(
                ("quiz.completed:v1:"+attemptId).getBytes(StandardCharsets.UTF_8)
        );
        jdbc.update(
                """
                INSERT INTO outbox_events
                    (event_id, aggregate_type, aggregate_id, event_type, event_version,
                     payload, trace_id, occurred_at, next_attempt_at)
                VALUES (?, 'QUIZ_ATTEMPT', ?::uuid, 'quiz.completed', 1,
                        '{"conflict":true}'::jsonb, 'conflict-test', now(), now())
                """,
                outboxEventId,attemptId
        );

        HttpResponse<String> response=answer(
                attemptId,quiz.q2(),quiz.q2Wrong(),"answer-2",USER_ID
        );

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.body()).contains("OUTBOX_EVENT_CONFLICT");
        assertThat(jdbc.queryForObject(
                "SELECT status FROM gameplay_attempts WHERE id = ?::uuid",
                String.class,attemptId
        )).isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM gameplay_answers WHERE attempt_id = ?::uuid",
                Integer.class,attemptId
        )).isEqualTo(1);
    }

    private QuizFixture publishedQuiz() throws Exception {
        String contentId=json(send("POST","/api/v1/admin/contents",
                "{\"title\":\"Gameplay Content\",\"contentType\":\"FILM\"}",EDITOR_ID,"EDITOR",null))
                .get("id").stringValue();
        UUID mediaAssetId=insertMediaAsset();
        assertThat(send("PUT","/api/v1/admin/contents/"+contentId+"/cover",
                "{\"mediaAssetId\":\""+mediaAssetId+"\",\"alternativeText\":\"Gameplay kapak gorseli\"}",
                EDITOR_ID,"EDITOR",null).statusCode()).isEqualTo(200);
        assertThat(send("POST","/api/v1/admin/contents/"+contentId+"/publish",
                null,EDITOR_ID,"EDITOR",null).statusCode()).isEqualTo(200);
        JsonNode created=json(send("POST","/api/v1/admin/quizzes",
                "{\"contentId\":\""+contentId+"\",\"title\":\"Gameplay Quiz\"}",EDITOR_ID,"EDITOR",null));
        String quizId=created.get("id").stringValue(), versionId=created.get("versions").get(0).get("id").stringValue();
        JsonNode q1=json(send("POST",adminQuestions(quizId,versionId),questionBody(1,"Soru 1"),EDITOR_ID,"EDITOR",null))
                .get("versions").get(0).get("questions").get(0);
        JsonNode q2=json(send("POST",adminQuestions(quizId,versionId),questionBody(2,"Soru 2"),EDITOR_ID,"EDITOR",null))
                .get("versions").get(0).get("questions").get(1);
        send("POST","/api/v1/admin/quizzes/"+quizId+"/versions/"+versionId+"/publish",null,EDITOR_ID,"EDITOR",null);
        return new QuizFixture(quizId,q1.get("id").stringValue(),q1.get("answerOptions").get(0).get("id").stringValue(),
                q1.get("answerOptions").get(1).get("id").stringValue(),q2.get("id").stringValue(),
                q2.get("answerOptions").get(0).get("id").stringValue(),q2.get("answerOptions").get(1).get("id").stringValue());
    }
    private UUID insertMediaAsset(){
        UUID mediaAssetId=UUID.randomUUID();
        jdbc.update("""
                INSERT INTO media_assets
                    (id, storage_key, media_type, mime_type, byte_size, checksum_sha256,
                     width, height, created_by, created_at)
                VALUES (?, ?, 'IMAGE', 'image/png', 1, ?, 1, 1, ?, now())
                """,mediaAssetId,mediaAssetId+".png","0".repeat(64),EDITOR_ID);
        return mediaAssetId;
    }
    private void consumeCompletionEvent(String attemptId) {
        String payload=jdbc.queryForObject(
                "SELECT payload::text FROM outbox_events WHERE aggregate_id = ?::uuid",
                String.class,attemptId
        );
        xpEventHandler.handle(payload);
    }
    private String adminQuestions(String q,String v){return "/api/v1/admin/quizzes/"+q+"/versions/"+v+"/questions";}
    private String questionBody(int order,String prompt){return "{\"questionOrder\":"+order+",\"prompt\":\""+prompt+"\",\"difficulty\":\"EASY\",\"answerOptions\":[{\"optionOrder\":1,\"text\":\"A\",\"correct\":true},{\"optionOrder\":2,\"text\":\"B\",\"correct\":false}]}";}
    private HttpResponse<String> answer(String a,String q,String o,String key,UUID user)throws Exception{return send("POST","/api/v1/attempts/"+a+"/answers","{\"questionId\":\""+q+"\",\"selectedOptionId\":\""+o+"\"}",user,"USER",key);}
    private HttpResponse<String> uncheckedAnswer(String a,String q,String o,String key){try{return answer(a,q,o,key,USER_ID);}catch(Exception e){throw new RuntimeException(e);}}
    private HttpResponse<String> uncheckedSend(String method,String path,String body,UUID actor,String role,String key){try{return send(method,path,body,actor,role,key);}catch(Exception e){throw new RuntimeException(e);}}
    private HttpResponse<String> send(String method,String path,String body,UUID actor,String role,String key)throws Exception{
        HttpRequest.Builder b=HttpRequest.newBuilder().uri(URI.create("http://localhost:"+port+path))
                .header(TemporaryHeaderAuthenticationFilter.ACTOR_ID_HEADER,actor.toString())
                .header(TemporaryHeaderAuthenticationFilter.ACTOR_ROLES_HEADER,role);
        if(key!=null)b.header("Idempotency-Key",key);
        if(body==null)b.method(method,HttpRequest.BodyPublishers.noBody());else b.header("Content-Type","application/json").method(method,HttpRequest.BodyPublishers.ofString(body));
        return client.send(b.build(),HttpResponse.BodyHandlers.ofString());
    }
    private JsonNode json(HttpResponse<String> r){return mapper.readTree(r.body());}
    private record QuizFixture(String quizId,String q1,String q1Correct,String q1Wrong,String q2,String q2Correct,String q2Wrong){}
}
