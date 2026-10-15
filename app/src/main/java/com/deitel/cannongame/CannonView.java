// CannonView.java
// Exibe e controla o Cannon Game (Figs. 6.19–6.34)
package com.deitel.cannongame;

import android.app.Activity;
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
import androidx.fragment.app.FragmentManager;

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
      int targetY = (int) ((0.5 - TARGET_LENGTH_PERCENT / 2) *
         screenHeight);

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
         targetX += (TARGET_WIDTH_PERCENT + TARGET_SPACING_PERCENT) *
            screenWidth;
      }

      // cria um novo Blocker
      blocker = new Blocker(this, Color.BLACK, MISS_PENALTY,
         (int) (BLOCKER_X_PERCENT * screenWidth),
         (int) ((0.5 - BLOCKER_LENGTH_PERCENT / 2) * screenHeight),
         (int) (BLOCKER_WIDTH_PERCENT * screenWidth),
         (int) (BLOCKER_LENGTH_PERCENT * screenHeight),
         (float) (BLOCKER_SPEED_PERCENT * screenHeight));

      timeLeft = 10; // inicia a contagem regressiva em 10 segundos

      shotsFired = 0; // define o número inicial de tiros
      totalElapsedTime = 0.0; // zera o tempo decorrido

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
      double interval = elapsedTimeMS / 1000.0; // converte para segundos

      // atualiza a posição da bala se ela estiver na tela
      if (cannon.getCannonball() != null)
         cannon.getCannonball().update(interval);

      blocker.update(interval); // atualiza a posição do blocker

      for (GameElement target : targets)
         target.update(interval); // atualiza a posição do alvo

      particles.update(interval); // EXTRA: atualiza as partículas

      timeLeft -= interval; // subtrai do tempo restante

      // se o cronômetro chegou a zero
      if (timeLeft <= 0) {
         timeLeft = 0.0;
         endGame(false); // derrota
      }
      // se todos os alvos foram atingidos
      else if (targets.isEmpty()) {
         endGame(true); // vitória
      }
   }

   // encerra o jogo uma única vez (chamado na thread do game loop)
   private void endGame(boolean won) {
      if (gameOver) return;
      gameOver = true; // o jogo terminou
      cannonThread.setRunning(false); // termina a thread

      // EXTRA: bônus de tempo na vitória, ranking local e som final
      if (won)
         score += (int) (timeLeft * WIN_TIME_BONUS_POINTS);
      int rank = highScores.addScore(score);
      bestScore = highScores.getBestScore();
      playSound(won ? WIN_SOUND_ID : LOSE_SOUND_ID);

      showGameOverDialog(won ? R.string.win : R.string.lose, rank);
   }

   // alinha o cano e dispara uma bala se não houver outra na tela
   public void alignAndFireCannonball(MotionEvent event) {
      // obtém o local do toque nesta view
      Point touchPoint = new Point((int) event.getX(),
         (int) event.getY());

      // calcula a distância vertical do toque ao centro da tela
      double centerMinusY = (screenHeight / 2 - touchPoint.y);

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

   // exibe um AlertDialog quando o jogo termina
   private void showGameOverDialog(final int messageId, int rank) {
      final String title = getResources().getString(messageId);

      // número de tiros, tempo total, pontuação e ranking
      StringBuilder message = new StringBuilder(getResources().getString(
         R.string.results_format, shotsFired, totalElapsedTime));
      message.append('\n').append(getResources().getString(
         R.string.score_results_format, score, bestScore));
      if (rank == 1)
         message.append('\n').append(getResources().getString(R.string.new_record));
      else if (rank > 1)
         message.append('\n').append(getResources().getString(
            R.string.top_rank_format, rank));
      final String text = message.toString();

      // na thread da GUI, usa o FragmentManager para exibir o DialogFragment
      activity.runOnUiThread(new Runnable() {
         public void run() {
            pauseMusic();
            showSystemBars(); // sai do modo imersivo

            FragmentManager fm = activity.getSupportFragmentManager();
            if (activity.isFinishing() || activity.isDestroyed() ||
               fm.isStateSaved()) {
               dialogIsDisplayed = false; // Activity em segundo plano
               return; // um novo jogo começará quando ela voltar
            }
            dialogIsDisplayed = true;
            GameResultDialogFragment.newInstance(title, text)
               .show(fm, "results");
         }
      });
   }

   // usado pelo diálogo para saber se ele ainda é válido
   public boolean isGameOverDialogExpected() {
      return dialogIsDisplayed;
   }

   // chamado pelo botão "Reset Game" do diálogo (Fig. 6.27)
   @Override
   public void onResetGame() {
      dialogIsDisplayed = false;
      newGame(); // configura e inicia um novo jogo
   }

   // desenha o jogo no Canvas recebido
   public void drawGameElements(Canvas canvas) {
      // limpa o fundo
      canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(),
         backgroundPaint);

      // exibe o tempo restante
      canvas.drawText(getResources().getString(
         R.string.time_remaining_format, timeLeft), 50, 100, textPaint);

      // EXTRA: HUD de pontuação, recorde e combo (canto superior direito)
      float lineHeight = textPaint.getTextSize() * 1.2f;
      float rightX = canvas.getWidth() - 50;
      scorePaint.setColor(Color.BLACK);
      canvas.drawText(getResources().getString(R.string.score_format, score),
         rightX, 100, scorePaint);
      canvas.drawText(getResources().getString(R.string.best_format,
         Math.max(bestScore, score)), rightX, 100 + lineHeight, scorePaint);
      if (combo > 1) {
         scorePaint.setColor(getResources().getColor(R.color.combo,
            getContext().getTheme()));
         canvas.drawText(getResources().getString(R.string.combo_format,
            Math.min(combo, MAX_MULTIPLIER)), rightX, 100 + 2 * lineHeight,
            scorePaint);
      }

      cannon.draw(canvas); // desenha o canhão

      // desenha os GameElements
      if (cannon.getCannonball() != null &&
         cannon.getCannonball().isOnScreen())
         cannon.getCannonball().draw(canvas);

      blocker.draw(canvas); // desenha o blocker

      // desenha todos os Targets
      for (GameElement target : targets)
         target.draw(canvas);

      particles.draw(canvas); // EXTRA: desenha as partículas
   }

   // verifica se a bala colide com o Blocker ou com algum Target
   public void testForCollisions() {
      // remove qualquer alvo com o qual a bala colida
      if (cannon.getCannonball() != null &&
         cannon.getCannonball().isOnScreen()) {
         for (int n = 0; n < targets.size(); n++) {
            if (cannon.getCannonball().collidesWith(targets.get(n))) {
               targets.get(n).playSound(); // som de acerto no alvo

               // soma a recompensa de acerto ao tempo restante
               timeLeft += targets.get(n).getHitReward();

               registerTargetHit(targets.get(n)); // EXTRA: pontos e partículas

               cannon.removeCannonball(); // remove a bala do jogo
               targets.remove(n); // remove o alvo atingido
               --n; // garante que o novo alvo n seja testado
               break;
            }
         }
      }
      else { // remove a bala se ela não deve mais estar na tela
         if (cannon.getCannonball() != null)
            combo = 0; // EXTRA: a bala saiu da tela sem acertar (errou)
         cannon.removeCannonball();
      }

      // verifica se a bala colide com o blocker
      if (cannon.getCannonball() != null &&
         cannon.getCannonball().collidesWith(blocker)) {
         blocker.playSound(); // som de acerto no blocker

         // inverte a direção da bala
         cannon.getCannonball().reverseVelocityX();

         // desconta a penalidade do blocker do tempo restante
         timeLeft -= blocker.getMissPenalty();

         // EXTRA: perde o combo e solta faíscas cinza
         combo = 0;
         particles.spawn(cannon.getCannonball().shape.centerX(),
            cannon.getCannonball().shape.centerY(), Color.DKGRAY, 10,
            screenHeight * 0.4f, screenHeight * 0.008f);
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

      if (!dialogIsDisplayed) {
         // gameOver = false evita que newGame() inicie uma segunda thread
         gameOver = false;
         newGame(); // configura e inicia um novo jogo
         startGameThread(); // cria e inicia a thread do game loop
      }
      else {
         drawSingleFrame(holder); // EXTRA: mostra o último quadro sob o diálogo
      }
   }

   // desenha um único quadro (sem thread ativa)
   private void drawSingleFrame(SurfaceHolder holder) {
      if (cannon == null || targets == null) return;
      Canvas canvas = holder.lockCanvas();
      if (canvas == null) return;
      try {
         drawGameElements(canvas);
      } finally {
         holder.unlockCanvasAndPost(canvas);
      }
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
      // obtém o int que representa o tipo de ação que gerou o evento
      int action = e.getAction();

      // o usuário tocou a tela ou arrastou o dedo sobre ela
      if (action == MotionEvent.ACTION_DOWN ||
         action == MotionEvent.ACTION_MOVE) {
         // dispara a bala em direção ao ponto tocado
         alignAndFireCannonball(e);
      }
      else if (action == MotionEvent.ACTION_UP) {
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
         Canvas canvas = null; // usado para desenhar
         long previousFrameTime = System.currentTimeMillis();

         while (threadIsRunning) {
            long frameStart = System.currentTimeMillis();
            try {
               // obtém o Canvas para desenho exclusivo desta thread
               canvas = surfaceHolder.lockCanvas(null);

               if (canvas != null) {
                  // trava o surfaceHolder para desenhar
                  synchronized (surfaceHolder) {
                     long currentTime = System.currentTimeMillis();
                     double elapsedTimeMS = currentTime - previousFrameTime;
                     totalElapsedTime += elapsedTimeMS / 1000.0;
                     updatePositions(elapsedTimeMS); // atualiza o estado
                     testForCollisions(); // testa colisões
                     drawGameElements(canvas); // desenha com o canvas
                     previousFrameTime = currentTime; // atualiza o tempo
                  }
               }
               else {
                  // Surface ainda indisponível: não conta esse tempo
                  previousFrameTime = System.currentTimeMillis();
               }
            }
            catch (IllegalStateException | IllegalArgumentException e) {
               Log.e(TAG, "Erro ao acessar o Canvas", e);
               canvas = null;
            }
            finally {
               // exibe o conteúdo do canvas no CannonView e libera o Canvas
               if (canvas != null)
                  surfaceHolder.unlockCanvasAndPost(canvas);
               canvas = null;
            }

            // limita a ~60 quadros/s: evita uso de 100% da CPU e passos de
            // tempo minúsculos (que truncariam o movimento a 0 pixel)
            long frameDuration = System.currentTimeMillis() - frameStart;
            if (frameDuration < FRAME_TIME_MS) {
               try {
                  Thread.sleep(FRAME_TIME_MS - frameDuration);
               }
               catch (InterruptedException e) {
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
