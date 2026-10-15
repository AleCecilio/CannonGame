// CannonView.java
// Exibe e controla o Cannon Game (Figs. 6.19–6.34)
package com.deitel.cannongame;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Shader;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.FragmentActivity;

import java.util.ArrayList;
import java.util.Random;

public class CannonView extends SurfaceView
        implements SurfaceHolder.Callback, GameResultDialogFragment.ResetListener {

   private static final String TAG = "CannonView"; // para registrar erros

   // constantes do jogo
   public static final int MISS_PENALTY = 2; // segundos descontados num erro
   public static final int HIT_REWARD = 3; // segundos somados num acerto

   // constantes do canhão
   public static final double CANNON_BASE_RADIUS_PERCENT = 3.0 / 40;
   public static final double CANNON_BARREL_WIDTH_PERCENT = 3.0 / 40;
   public static final double CANNON_BARREL_LENGTH_PERCENT = 1.0 / 10;

   // constantes da bala
   public static final double CANNONBALL_RADIUS_PERCENT = 3.0 / 80;
   public static final double CANNONBALL_SPEED_PERCENT = 3.0 / 2;

   // constantes dos alvos
   public static final double TARGET_WIDTH_PERCENT = 1.0 / 40;
   public static final double TARGET_LENGTH_PERCENT = 3.0 / 20;
   public static final double TARGET_FIRST_X_PERCENT = 3.0 / 5;
   public static final double TARGET_SPACING_PERCENT = 1.0 / 60;
   public static final double TARGET_PIECES = 9;
   public static final double TARGET_MIN_SPEED_PERCENT = 3.0 / 4;
   public static final double TARGET_MAX_SPEED_PERCENT = 6.0 / 4;

   // constantes do blocker
   public static final double BLOCKER_WIDTH_PERCENT = 1.0 / 40;
   public static final double BLOCKER_LENGTH_PERCENT = 1.0 / 4;
   public static final double BLOCKER_X_PERCENT = 1.0 / 2;
   public static final double BLOCKER_SPEED_PERCENT = 1.0;

   // tamanho do texto: 1/18 da altura da tela
   public static final double TEXT_SIZE_PERCENT = 1.0 / 18;

   // EXTRA: constantes de pontuação
   public static final int BASE_POINTS = 100; // pontos por alvo
   public static final int MAX_MULTIPLIER = 5; // multiplicador máximo de combo
   public static final int WIN_TIME_BONUS_POINTS = 10; // por segundo sobrando

   // duração mínima de cada quadro do game loop (~60 quadros por segundo)
   private static final long FRAME_TIME_MS = 16;

   private CannonThread cannonThread; // controla o game loop
   private FragmentActivity activity; // para exibir o diálogo na thread da GUI
   private volatile boolean dialogIsDisplayed = false;
   private volatile boolean surfaceReady = false; // existe uma Surface válida?

   // objetos do jogo
   private Cannon cannon;
   private Blocker blocker;
   private ArrayList<Target> targets;

   // dimensões
   private int screenWidth;
   private int screenHeight;

   // variáveis do game loop e das estatísticas
   private volatile boolean gameOver = true; // o jogo terminou?
   private double timeLeft; // tempo restante em segundos
   private int shotsFired; // tiros disparados pelo usuário
   private double totalElapsedTime; // segundos decorridos
   private double difficultyMultiplier = 1.0;
   private volatile boolean isNightMode = false;

   // EXTRA: pontuação, combo, ranking e partículas
   private int score;
   private int combo;
   private int bestScore;
   private final HighScoreStore highScores;
   private final ParticleSystem particles = new ParticleSystem();

   // constantes e variáveis para o gerenciamento dos sons
   public static final int TARGET_SOUND_ID = 0;
   public static final int CANNON_SOUND_ID = 1;
   public static final int BLOCKER_SOUND_ID = 2;
   public static final int WIN_SOUND_ID = 3; // EXTRA
   public static final int LOSE_SOUND_ID = 4; // EXTRA
   private volatile SoundPool soundPool; // reproduz os efeitos sonoros
   private SparseIntArray soundMap; // mapeia IDs para o SoundPool
   private MediaPlayer backgroundMusic; // EXTRA: trilha sonora contínua

   // variáveis Paint usadas ao desenhar cada item na tela
   private Paint textPaint; // Paint do texto
   private Paint scorePaint; // EXTRA: Paint do HUD alinhado à direita
   private Paint backgroundPaint; // Paint usado para limpar a área de desenho
   private Paint overlayPaint; // Paint para a tela escurecida de Game Over
   private Paint gameOverPaint; // Paint para os textos da tela de Game Over

   // construtor
   public CannonView(Context context, AttributeSet attrs) {
      super(context, attrs); // chama o construtor da superclasse
      activity = findActivity(context); // referência à MainActivity

      // registra o listener SurfaceHolder.Callback
      getHolder().addCallback(this);

      // configura os atributos de áudio do jogo
      AudioAttributes.Builder attrBuilder = new AudioAttributes.Builder();
      attrBuilder.setUsage(AudioAttributes.USAGE_GAME);

      // inicializa o SoundPool que reproduz os efeitos sonoros do app.
      // O livro usa 1 stream; usamos 3 para que os sons dos extras
      // (vitória/derrota) não cortem o som do último acerto.
      SoundPool.Builder builder = new SoundPool.Builder();
      builder.setMaxStreams(3);
      builder.setAudioAttributes(attrBuilder.build());
      soundPool = builder.build();

      // cria o mapa de sons e os pré-carrega
      soundMap = new SparseIntArray(5);
      soundMap.put(TARGET_SOUND_ID,
              soundPool.load(context, R.raw.target_hit, 1));
      soundMap.put(CANNON_SOUND_ID,
              soundPool.load(context, R.raw.cannon_fire, 1));
      soundMap.put(BLOCKER_SOUND_ID,
              soundPool.load(context, R.raw.blocker_hit, 1));
      soundMap.put(WIN_SOUND_ID,
              soundPool.load(context, R.raw.win_jingle, 1));
      soundMap.put(LOSE_SOUND_ID,
              soundPool.load(context, R.raw.lose_jingle, 1));

      // EXTRA: trilha sonora em loop
      backgroundMusic = MediaPlayer.create(context, R.raw.bg_music);
      if (backgroundMusic != null) {
         backgroundMusic.setLooping(true);
         backgroundMusic.setVolume(0.35f, 0.35f);
      }

      highScores = new HighScoreStore(context);

      textPaint = new Paint();
      scorePaint = new Paint();
      scorePaint.setTextAlign(Paint.Align.RIGHT);
      scorePaint.setAntiAlias(true);
      backgroundPaint = new Paint();
      backgroundPaint.setColor(Color.WHITE);

      overlayPaint = new Paint();
      overlayPaint.setColor(Color.argb(180, 0, 0, 0));

      gameOverPaint = new Paint();
      gameOverPaint.setAntiAlias(true);
      gameOverPaint.setTextAlign(Paint.Align.CENTER);
   }

   // obtém a Activity que contém o contexto (o contexto pode estar "embrulhado")
   private static FragmentActivity findActivity(Context context) {
      while (context instanceof ContextWrapper) {
         if (context instanceof FragmentActivity)
            return (FragmentActivity) context;
         context = ((ContextWrapper) context).getBaseContext();
      }
      throw new IllegalStateException("CannonView deve estar em uma FragmentActivity");
   }

   // chamado quando o tamanho da SurfaceView muda, como na primeira inflação
   @Override
   protected void onSizeChanged(int w, int h, int oldw, int oldh) {
      super.onSizeChanged(w, h, oldw, oldh);
      configureForSize(w, h);
   }

   // guarda as dimensões e configura os Paints que dependem delas
   private void configureForSize(int w, int h) {
      screenWidth = w; // armazena a largura do CannonView
      screenHeight = h; // armazena a altura do CannonView

      // configura as propriedades do texto
      textPaint.setTextSize((int) (TEXT_SIZE_PERCENT * screenHeight));
      textPaint.setAntiAlias(true); // suaviza o texto
      scorePaint.setTextSize((int) (TEXT_SIZE_PERCENT * screenHeight));

      // EXTRA: fundo em degradê (céu)
      backgroundPaint.setShader(new LinearGradient(0, 0, 0, h,
              getResources().getColor(R.color.sky_top, getContext().getTheme()),
              getResources().getColor(R.color.sky_bottom, getContext().getTheme()),
              Shader.TileMode.CLAMP));
   }

   // obtém a largura da tela do jogo
   public int getScreenWidth() {
      return screenWidth;
   }

   // obtém a altura da tela do jogo
   public int getScreenHeight() {
      return screenHeight;
   }

   // indica se o modo noturno está ativo
   public boolean isNightMode() {
      return isNightMode;
   }

   // reproduz o som com o soundId recebido em soundMap
   public void playSound(int soundId) {
      SoundPool pool = soundPool; // pode ser liberado por outra thread
      if (pool != null)
         pool.play(soundMap.get(soundId), 1, 1, 1, 0, 1f);
   }

   // reinicia todos os elementos e começa um novo jogo
   public void newGame() {
      // constrói um novo Cannon
      cannon = new Cannon(this,
              (int) (CANNON_BASE_RADIUS_PERCENT * screenHeight),
              (int) (CANNON_BARREL_LENGTH_PERCENT * screenWidth),
              (int) (CANNON_BARREL_WIDTH_PERCENT * screenHeight));

      Random random = new Random(); // para velocidades aleatórias
      targets = new ArrayList<>(); // constrói uma nova lista de Targets

      // inicializa targetX para o primeiro Target a partir da esquerda
      int targetX = (int) (TARGET_FIRST_X_PERCENT * screenWidth);

      // calcula a coordenada Y dos Targets
      int targetY = (int) ((0.5 - TARGET_LENGTH_PERCENT / 2.0) * screenHeight);

      // adiciona TARGET_PIECES Targets à lista
      for (int n = 0; n < TARGET_PIECES; n++) {

         // velocidade aleatória entre os valores mínimo e máximo
         double velocity = screenHeight * (random.nextDouble() *
                 (TARGET_MAX_SPEED_PERCENT - TARGET_MIN_SPEED_PERCENT) +
                 TARGET_MIN_SPEED_PERCENT);

         // alterna as cores dos Targets entre escura e clara
         int color = (n % 2 == 0) ?
                 getResources().getColor(R.color.dark,
                         getContext().getTheme()) :
                 getResources().getColor(R.color.light,
                         getContext().getTheme());

         velocity *= -1; // inverte a velocidade inicial do próximo Target

         // cria e adiciona um novo Target à lista
         targets.add(new Target(this, color, HIT_REWARD, targetX, targetY,
                 (int) (TARGET_WIDTH_PERCENT * screenWidth),
                 (int) (TARGET_LENGTH_PERCENT * screenHeight),
                 (int) velocity));

         // aumenta a coordenada x para posicionar o próximo Target
         targetX += (TARGET_WIDTH_PERCENT + TARGET_SPACING_PERCENT) * screenWidth;
      }

      // cria um novo Blocker
      blocker = new Blocker(this, Color.BLACK, MISS_PENALTY,
              (int) (BLOCKER_X_PERCENT * screenWidth),
              (int) ((0.5 - BLOCKER_LENGTH_PERCENT / 2.0) * screenHeight),
              (int) (BLOCKER_WIDTH_PERCENT * screenWidth),
              (int) (BLOCKER_LENGTH_PERCENT * screenHeight),
              (float) (BLOCKER_SPEED_PERCENT * screenHeight));

      timeLeft = 10; // inicia a contagem regressiva em 10 segundos

      shotsFired = 0; // define o número inicial de tiros
      totalElapsedTime = 0.0; // zera o tempo decorrido
      difficultyMultiplier = 1.0; // garante que difficultyMultiplier volte a ser 1.0

      // EXTRA: zera pontuação, combo e partículas
      score = 0;
      combo = 0;
      bestScore = highScores.getBestScore();
      particles.clear();

      if (gameOver) { // inicia um novo jogo depois do término do anterior
         gameOver = false; // o jogo não terminou
         startGameThread(); // cria e inicia a thread do game loop
      }

      restartMusic(); // EXTRA
      hideSystemBars();
   }

   // cria e inicia a CannonThread (ou a reativa, se ainda estiver viva)
   private void startGameThread() {
      if (cannonThread != null && cannonThread.isAlive()) {
         cannonThread.setRunning(true);
         return;
      }
      cannonThread = new CannonThread(getHolder()); // cria a thread
      cannonThread.setRunning(true);
      cannonThread.start(); // inicia a thread do game loop
   }

   // chamado repetidamente pela CannonThread para atualizar os elementos
   private void updatePositions(double elapsedTimeMS) {
      // trava tudo se der game over
      if (gameOver) return;

      double interval = elapsedTimeMS / 1000.0;

      // soma o tempo da partida
      totalElapsedTime += interval;

      // move a bala
      if (cannon.getCannonball() != null)
         cannon.getCannonball().update(interval);

      // move o blocker, alvos e as partículas
      blocker.update(interval);
      for (GameElement target : targets)
         target.update(interval);
      particles.update(interval);

      // desconta o relógio
      timeLeft -= interval;

      // verifica fim de jogo
      if (timeLeft <= 0) {
         timeLeft = 0.0;
         endGame(false); // perdeu
      }
      else if (targets.isEmpty()) {
         endGame(true); // ganhou
      }
   }

   // encerra o jogo uma única vez (chamado na thread do game loop)
   private void endGame(boolean won) {
      if (gameOver) return;
      gameOver = true; // o jogo terminou

      // EXTRA: bônus de tempo na vitória, ranking local e som final
      if (won)
         score += (int) (timeLeft * WIN_TIME_BONUS_POINTS);
      highScores.addScore(score);
      bestScore = highScores.getBestScore();
      playSound(won ? WIN_SOUND_ID : LOSE_SOUND_ID);
   }

   // alinha o cano e dispara uma bala se não houver outra na tela
   public void alignAndFireCannonball(MotionEvent event) {
      // obtém o local do toque nesta view
      Point touchPoint = new Point((int) event.getX(),
              (int) event.getY());

      // calcula a distância vertical do toque ao centro da tela
      double centerMinusY = (screenHeight / 2.0 - touchPoint.y);

      double angle = 0; // inicializa o ângulo com 0

      // calcula o ângulo do cano com a horizontal
      angle = Math.atan2(touchPoint.x, centerMinusY);

      // evita corrida com a thread do jogo, que também usa o holder
      synchronized (getHolder()) {
         if (cannon == null || gameOver) return;

         // aponta o cano para o local tocado
         cannon.align(angle);

         // dispara a bala se não houver outra na tela
         if (cannon.getCannonball() == null ||
                 !cannon.getCannonball().isOnScreen()) {
            cannon.fireCannonball();
            ++shotsFired;
         }
      }
   }

   // usado pelo diálogo para saber se ele ainda é válido
   public boolean isGameOverDialogExpected() {
      return false;
   }

   // chamado pelo botão "Reset Game" do diálogo (Fig. 6.27)
   @Override
   public void onResetGame() {
      newGame(); // configura e inicia um novo jogo
   }

   // desenha o jogo no Canvas recebido
   public void drawGameElements(Canvas canvas) {
      // pinta o fundo escuro ou claro
      if (isNightMode) {
         canvas.drawColor(Color.rgb(20, 24, 38));
         textPaint.setColor(Color.WHITE);
         scorePaint.setColor(Color.WHITE);
      } else {
         canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), backgroundPaint);
         textPaint.setColor(Color.BLACK);
         scorePaint.setColor(Color.BLACK);
      }

      // desenha o relógio
      canvas.drawText(getResources().getString(R.string.time_remaining_format, timeLeft), 50, 100, textPaint);

      // textos da direita (pontos, recorde e combo)
      float lineHeight = textPaint.getTextSize() * 1.2f;
      float rightX = canvas.getWidth() - 50;
      canvas.drawText(getResources().getString(R.string.score_format, score), rightX, 100, scorePaint);
      canvas.drawText(getResources().getString(R.string.best_format, Math.max(bestScore, score)), rightX, 100 + lineHeight, scorePaint);

      if (combo > 1) {
         scorePaint.setColor(getResources().getColor(R.color.combo, getContext().getTheme()));
         canvas.drawText(getResources().getString(R.string.combo_format, Math.min(combo, MAX_MULTIPLIER)), rightX, 100 + 2 * lineHeight, scorePaint);
      }

      // desenha o canhão e a bala
      cannon.draw(canvas);
      if (cannon.getCannonball() != null && cannon.getCannonball().isOnScreen())
         cannon.getCannonball().draw(canvas);

      // desenha o resto
      blocker.draw(canvas);
      for (GameElement target : targets)
         target.draw(canvas);
      particles.draw(canvas);

      // tela de game over
      if (gameOver) {
         // fundo escurecido
         canvas.drawRect(0, 0, screenWidth, screenHeight, overlayPaint);

         // título
         float titleSize = (float) (TEXT_SIZE_PERCENT * screenHeight * 1.5);
         gameOverPaint.setColor(Color.WHITE);
         gameOverPaint.setTextSize(titleSize);
         gameOverPaint.setFakeBoldText(true);

         float centerY = screenHeight / 2.0f - titleSize * 1.2f;
         String title = targets.isEmpty() ? "VITÓRIA!" : "GAME OVER";
         canvas.drawText(title, screenWidth / 2.0f, centerY, gameOverPaint);

         // status da partida
         float bodySize = (float) (TEXT_SIZE_PERCENT * screenHeight * 0.85);
         gameOverPaint.setTextSize(bodySize);
         gameOverPaint.setFakeBoldText(false);

         float lineSpacing = bodySize * 1.4f;
         centerY += lineSpacing * 1.4f;

         // quebra as linhas do texto de tempo/tiros no \n
         String timeStr = getResources().getString(R.string.results_format, shotsFired, totalElapsedTime);
         for (String linha : timeStr.split("\n")) {
            canvas.drawText(linha, screenWidth / 2.0f, centerY, gameOverPaint);
            centerY += lineSpacing;
         }

         centerY += lineSpacing * 0.5f; // dá um espacinho extra

         // quebra as linhas do texto de pontos
         String scoreStr = getResources().getString(R.string.score_results_format, score, bestScore);
         for (String linha : scoreStr.split("\n")) {
            canvas.drawText(linha, screenWidth / 2.0f, centerY, gameOverPaint);
            centerY += lineSpacing;
         }

         // faz o texto de reiniciar piscar
         int blinkAlpha = 152 + (int) (103 * Math.sin(System.currentTimeMillis() / 200.0));
         gameOverPaint.setColor(Color.YELLOW);
         gameOverPaint.setAlpha(blinkAlpha);
         gameOverPaint.setTextSize((float) (TEXT_SIZE_PERCENT * screenHeight * 0.8));

         centerY += lineSpacing * 1.5f;
         canvas.drawText("Toque na tela para reiniciar", screenWidth / 2.0f, centerY, gameOverPaint);
      }
   }

   // verifica se a bala colide com o Blocker ou com algum Target usando colisão circular avançada
   public void testForCollisions() {
      Cannonball ball = cannon.getCannonball();

      // remove qualquer alvo com o qual a bala colida
      if (ball != null && ball.isOnScreen()) {
         // Centro e raio da bala
         float ballCenterX = ball.shape.centerX();
         float ballCenterY = ball.shape.centerY();
         float ballRadius = ball.shape.width() / 2.0f;

         for (int n = 0; n < targets.size(); n++) {
            Target target = targets.get(n);

            // Centro e raio do alvo
            float targetCenterX = target.shape.centerX();
            float targetCenterY = target.shape.centerY();
            float targetRadius = target.shape.width() / 2.0f;

            // Diferença entre os eixos (dx, dy)
            float dx = ballCenterX - targetCenterX;
            float dy = ballCenterY - targetCenterY;
            float radiusSum = ballRadius + targetRadius;

            // Colisão circular usando distância ao quadrado: (dx^2 + dy^2) <= (r1 + r2)^2
            if ((dx * dx) + (dy * dy) <= (radiusSum * radiusSum)) {
               target.playSound(); // som de acerto no alvo

               // soma a recompensa de acerto ao tempo restante
               timeLeft += target.getHitReward();

               registerTargetHit(target); // EXTRA: pontos e partículas

               cannon.removeCannonball(); // remove a bala do jogo
               targets.remove(n); // remove o alvo atingido
               --n; // garante que o novo alvo n seja testado

               // Dificuldade Progressiva
               difficultyMultiplier += 0.10;
               for (Target t : targets) {
                  t.setVelocityY((float) (t.getVelocityY() * 1.10));
               }

               break;
            }
         }
      }
      else { // remove a bala se ela não deve mais estar na tela
         if (ball != null)
            combo = 0; // EXTRA: a bala saiu da tela sem acertar (errou)
         cannon.removeCannonball();
      }

      // verifica se a bala colide com o blocker usando interseção de retângulos (AABB)
      ball = cannon.getCannonball();
      if (ball != null && ball.isOnScreen()) {

         // O blocker é alto e reto, a colisão retangular nativa é a correta matematicamente
         if (android.graphics.Rect.intersects(ball.shape, blocker.shape)) {
            blocker.playSound(); // som de acerto no blocker

            // inverte a direção da bala
            ball.reverseVelocityX();

            // desconta a penalidade do blocker do tempo restante
            timeLeft -= blocker.getMissPenalty();

            // EXTRA: perde o combo e solta faíscas cinza
            combo = 0;
            particles.spawn(ball.shape.centerX(),
                    ball.shape.centerY(), Color.DKGRAY, 10,
                    screenHeight * 0.4f, screenHeight * 0.008f);
         }
      }
   }

   // EXTRA: soma pontos com multiplicador de combo e cria a explosão
   private void registerTargetHit(Target target) {
      ++combo;
      int multiplier = Math.min(combo, MAX_MULTIPLIER);
      score += BASE_POINTS * multiplier;

      particles.spawn(target.shape.centerX(), target.shape.centerY(),
              target.paint.getColor(), 24, screenHeight * 0.6f,
              screenHeight * 0.012f);
   }

   // para o jogo: chamado pelo onPause do MainActivityFragment
   public void stopGame() {
      if (cannonThread != null)
         cannonThread.setRunning(false); // pede à thread que termine
      pauseMusic();
   }

   // EXTRA: retoma o jogo quando a Activity volta ao primeiro plano sem que
   // a Surface tenha sido destruída (ex.: modo multi-janela). Se a Surface foi
   // destruída, surfaceCreated cuida de tudo, como no livro.
   public void resumeGame() {
      if (!surfaceReady || gameOver || dialogIsDisplayed || cannon == null)
         return;
      startGameThread();
      startMusic();
   }

   // libera recursos: chamado pelo onDestroy do MainActivityFragment
   public void releaseResources() {
      SoundPool pool = soundPool;
      soundPool = null;
      if (pool != null)
         pool.release(); // libera todos os recursos do SoundPool

      if (backgroundMusic != null) {
         backgroundMusic.release();
         backgroundMusic = null;
      }
   }

   // ----- trilha sonora (EXTRA); sempre chamada na thread da GUI -----
   private void startMusic() {
      try {
         if (backgroundMusic != null && !backgroundMusic.isPlaying())
            backgroundMusic.start();
      } catch (IllegalStateException e) {
         Log.e(TAG, "Não foi possível iniciar a música", e);
      }
   }

   private void restartMusic() {
      try {
         if (backgroundMusic != null) {
            backgroundMusic.seekTo(0);
            if (!backgroundMusic.isPlaying())
               backgroundMusic.start();
         }
      } catch (IllegalStateException e) {
         Log.e(TAG, "Não foi possível reiniciar a música", e);
      }
   }

   private void pauseMusic() {
      try {
         if (backgroundMusic != null && backgroundMusic.isPlaying())
            backgroundMusic.pause();
      } catch (IllegalStateException e) {
         Log.e(TAG, "Não foi possível pausar a música", e);
      }
   }

   // chamado quando a superfície muda de tamanho
   @Override
   public void surfaceChanged(SurfaceHolder holder, int format,
                              int width, int height) { }

   // chamado quando a superfície é criada pela primeira vez
   @Override
   public void surfaceCreated(SurfaceHolder holder) {
      surfaceReady = true;
      if (screenWidth == 0 || screenHeight == 0)
         configureForSize(getWidth(), getHeight()); // por segurança

      gameOver = false;
      newGame(); // configura e inicia um novo jogo
      startGameThread(); // cria e inicia a thread do game loop
   }

   // chamado quando a superfície é destruída
   @Override
   public void surfaceDestroyed(SurfaceHolder holder) {
      surfaceReady = false;
      pauseMusic();

      // garante que a thread termine corretamente
      if (cannonThread != null) {
         boolean retry = true;
         cannonThread.setRunning(false); // termina a cannonThread

         while (retry) {
            try {
               cannonThread.join(); // espera a cannonThread terminar
               retry = false;
            }
            catch (InterruptedException e) {
               Log.e(TAG, "Thread interrupted", e);
            }
         }
      }
   }

   // chamado quando o usuário toca a tela
   @Override
   public boolean onTouchEvent(MotionEvent e) {
      int actionMasked = e.getActionMasked();

      // Intercepta o toque para reiniciar a partida quando em Game Over
      if (gameOver) {
         if (actionMasked == MotionEvent.ACTION_DOWN) {
            newGame(); // reinicia a partida diretamente
         }
         return true;
      }

      // Gatilho Multitouch: alterna o Modo Noturno ao tocar com 2 ou mais dedos
      if (e.getPointerCount() > 1 && actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
         isNightMode = !isNightMode;
         return true;
      }

      // o usuário tocou a tela ou arrastou o dedo sobre ela
      if (actionMasked == MotionEvent.ACTION_DOWN ||
              actionMasked == MotionEvent.ACTION_MOVE) {
         // dispara a bala em direção ao ponto tocado
         alignAndFireCannonball(e);
      }
      else if (actionMasked == MotionEvent.ACTION_UP) {
         performClick(); // acessibilidade
      }

      return true;
   }

   @Override
   public boolean performClick() {
      return super.performClick();
   }

   // Subclasse de Thread que controla o game loop
   private class CannonThread extends Thread {
      private final SurfaceHolder surfaceHolder; // para manipular o canvas
      private volatile boolean threadIsRunning = true; // executando por padrão

      // inicializa o SurfaceHolder
      public CannonThread(SurfaceHolder holder) {
         surfaceHolder = holder;
         setName("CannonThread");
      }

      // altera o estado de execução
      public void setRunning(boolean running) {
         threadIsRunning = running;
      }

      // controla o game loop
      @Override
      public void run() {
         Canvas canvas = null;
         long previousFrameTime = System.currentTimeMillis();

         while (threadIsRunning) {
            long frameStart = System.currentTimeMillis();
            try {
               // pega o canvas pra desenhar
               canvas = surfaceHolder.lockCanvas(null);

               if (canvas != null) {
                  // trava a tela pra atualizar
                  synchronized (surfaceHolder) {
                     long currentTime = System.currentTimeMillis();
                     double elapsedTimeMS = currentTime - previousFrameTime;

                     updatePositions(elapsedTimeMS);
                     testForCollisions();
                     drawGameElements(canvas);

                     previousFrameTime = currentTime;
                  }
               } else {
                  previousFrameTime = System.currentTimeMillis();
               }
            } catch (IllegalStateException | IllegalArgumentException e) {
               Log.e(TAG, "Erro ao acessar o Canvas", e);
               canvas = null;
            } finally {
               // libera e mostra na tela
               if (canvas != null)
                  surfaceHolder.unlockCanvasAndPost(canvas);
               canvas = null;
            }

            // segura a taxa de fps em 60
            long frameDuration = System.currentTimeMillis() - frameStart;
            if (frameDuration < FRAME_TIME_MS) {
               try {
                  Thread.sleep(FRAME_TIME_MS - frameDuration);
               } catch (InterruptedException e) {
                  Log.e(TAG, "Thread interrupted", e);
               }
            }
         }
      }
   }

   // oculta as barras do sistema (modo imersivo). O livro usa
   // setSystemUiVisibility, obsoleto desde a API 30; WindowInsetsController
   // produz o mesmo efeito: barras ocultas que reaparecem com um deslize.
   private void hideSystemBars() {
      Window window = activity.getWindow();
      WindowCompat.setDecorFitsSystemWindows(window, false);
      WindowInsetsControllerCompat controller =
              WindowCompat.getInsetsController(window, this);
      controller.setSystemBarsBehavior(
              WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
      controller.hide(WindowInsetsCompat.Type.systemBars());
   }

   // exibe as barras do sistema
   private void showSystemBars() {
      Window window = activity.getWindow();
      WindowCompat.setDecorFitsSystemWindows(window, false);
      WindowInsetsControllerCompat controller =
              WindowCompat.getInsetsController(window, this);
      controller.show(WindowInsetsCompat.Type.systemBars());
   }
}