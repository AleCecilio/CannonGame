// HighScoreStore.java
// EXTRA: ranking local (Top 5) persistido com SharedPreferences.
package com.deitel.cannongame;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HighScoreStore {
   public static final int MAX_ENTRIES = 5;
   private static final String PREFS_NAME = "cannon_game_prefs";
   private static final String KEY_TOP_SCORES = "top_scores";

   private final SharedPreferences prefs;

   public HighScoreStore(Context context) {
      prefs = context.getApplicationContext()
         .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
   }

   // retorna as pontuações em ordem decrescente
   public synchronized List<Integer> getTopScores() {
      List<Integer> scores = new ArrayList<>();
      String saved = prefs.getString(KEY_TOP_SCORES, "");
      if (!saved.isEmpty()) {
         for (String part : saved.split(",")) {
            try {
               scores.add(Integer.parseInt(part.trim()));
            } catch (NumberFormatException e) {
               // ignora entrada inválida
            }
         }
      }
      Collections.sort(scores, Collections.reverseOrder());
      return scores;
   }

   // melhor pontuação (0 se não houver)
   public synchronized int getBestScore() {
      List<Integer> scores = getTopScores();
      return scores.isEmpty() ? 0 : scores.get(0);
   }

   // registra a pontuação; retorna a posição no ranking (1 a 5)
   // ou -1 se ela não entrou no Top 5
   public synchronized int addScore(int score) {
      if (score <= 0) return -1;
      List<Integer> scores = getTopScores();
      int rank = 1;
      for (int s : scores) {
         if (score > s) break;
         rank++;
      }
      if (rank > MAX_ENTRIES) return -1;

      scores.add(rank - 1, score);
      while (scores.size() > MAX_ENTRIES) scores.remove(scores.size() - 1);

      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < scores.size(); i++) {
         if (i > 0) sb.append(',');
         sb.append(scores.get(i));
      }
      prefs.edit().putString(KEY_TOP_SCORES, sb.toString()).apply();
      return rank;
   }
}
