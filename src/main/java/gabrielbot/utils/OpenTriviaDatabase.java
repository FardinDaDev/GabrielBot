package gabrielbot.utils;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

public class OpenTriviaDatabase {
    private volatile static String token;

    public static void resetToken() throws UnirestException {
        token = Unirest.get("https://opentdb.com/api_token.php?command=request")
                .asJson()
                .getBody()
                .getObject()
                .getString("token");
    }

    public static Question random() {
        Question[] q = query("amount=1", 5);
        return q.length == 0 ? null : q[0];
    }

    public static Question[] query(String query, int attempts) {
        Question[] ret;
        for(int i = 0; i < attempts; i++) {
            ret = query(query);
            if(ret.length > 0) return ret;
        }
        return new Question[0];
    }

    public static Question[] query(String query) {
        try {
            JSONObject obj = Unirest.get("https://opentdb.com/api.php?" + query + "&encode=base64&token=" + token)
                    .asJson()
                    .getBody()
                    .getObject();
            int responseCode = obj.getInt("response_code");
            switch(responseCode) {
                case 1:
                case 2:
                    return new Question[0];
                case 3:
                case 4:
                    resetToken();
                    return query(query);
            }
            JSONArray questions = obj.getJSONArray("results");
            Question[] ret = new Question[questions.length()];
            for(int i = 0; i < ret.length; i++) {
                JSONObject q = questions.getJSONObject(i);
                ret[i] = new Question(
                        fromB64(q.getString("difficulty")),
                        fromB64(q.getString("type")),
                        fromB64(q.getString("category")),
                        fromB64(q.getString("question")),
                        fromB64(q.getString("correct_answer")),
                        q.getJSONArray("incorrect_answers")
                );
            }
            return ret;
        } catch(UnirestException e) {
            return new Question[0];
        }
    }

    private static String fromB64(String b64) {
        return new String(Base64.getDecoder().decode(b64), StandardCharsets.UTF_8);
    }

    public static class Question {
        public final String difficulty;
        public final String type;
        public final String category;
        public final String question;
        public final String correctAnswer;
        public final List<String> incorrectAnswers;

        public Question(String difficulty, String type, String category, String question, String correctAnswer, JSONArray incorrectAnswers) {
            this.difficulty = difficulty;
            this.type = type;
            this.category = category;
            this.question = question;
            this.correctAnswer = correctAnswer;
            this.incorrectAnswers = incorrectAnswers.toList().stream().map(v->fromB64(String.valueOf(v))).collect(Collectors.toList());
        }
    }
}
