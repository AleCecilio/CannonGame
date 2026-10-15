# Cannon Game

Aplicação Android do jogo **Cannon Game**, desenvolvida para a disciplina de **Programação III** a partir do **Capítulo 6 – Cannon Game App** do livro *Android 6 for Programmers: An App-Driven Approach* (3ª edição, Deitel).

## Integrantes

- [NOME DO INTEGRANTE 1]
- [NOME DO INTEGRANTE 2]
- [NOME DO INTEGRANTE 3]

## Objetivo

Aplicar na prática os conceitos fundamentais de programação mobile estudados no Capítulo 6: gráficos 2D com `Canvas`/`Paint`, animação quadro a quadro, game loop em thread própria sobre `SurfaceView`/`SurfaceHolder`, eventos de toque, efeitos sonoros e modo imersivo (tela cheia).

**Como jogar:** toque (ou arraste) na tela para apontar o canhão e disparar. Destrua os 9 alvos antes de o tempo (10 s) acabar. Cada alvo atingido soma 3 s; bater no bloqueador (barra preta) desconta 2 s e devolve a bala.

## Funcionalidades

- Jogo completo: canhão, bala, 9 alvos móveis, bloqueador móvel, cronômetro, vitória e derrota.
- Colisões bala × alvo e bala × bloqueador, com recompensa/penalidade de tempo.
- Efeitos sonoros: disparo, acerto no alvo e acerto no bloqueador.
- Diálogo de fim de jogo (tiros disparados, tempo total) com botão **Reset Game**.
- Orientação paisagem fixa, tela cheia e modo imersivo.
- Tratamento do ciclo de vida: pausa, retorno do segundo plano, destruição/recriação da `Surface`; a thread do jogo sempre é encerrada.

## Conceitos utilizados (e onde estão)

| Conceito | Onde |
|---|---|
| `SurfaceView` / `SurfaceHolder.Callback` | `CannonView` (`surfaceCreated`, `surfaceChanged`, `surfaceDestroyed`) |
| `Canvas` e `Paint` | `CannonView.drawGameElements`, `GameElement.draw`, `Cannon.draw`, `Cannonball.draw` |
| `Thread` + Game Loop | `CannonView.CannonThread.run()` (`lockCanvas` → atualizar → colisões → desenhar → `unlockCanvasAndPost`) |
| Animação quadro a quadro | `updatePositions(elapsedTimeMS)` move cada elemento conforme `velocidade × intervalo` a cada quadro |
| Touch Events | `CannonView.onTouchEvent` → `alignAndFireCannonball` |
| Sons | `SoundPool` (efeitos) em `CannonView`; arquivos em `res/raw` |
| Modo imersivo / tela cheia | Tema `windowFullscreen` + `hideSystemBars()` (WindowInsetsController) |
| `DialogFragment` | `GameResultDialogFragment` |
| Ciclo de vida | `MainActivityFragment.onPause/onResume/onDestroy` → `stopGame/resumeGame/releaseResources` |

### Correspondência com o Capítulo 6

| Seção do capítulo | Arquivo |
|---|---|
| 6.4.x Activity e Fragment | `MainActivity`, `MainActivityFragment` |
| `GameElement`, `Blocker`, `Target` | `GameElement.java`, `Blocker.java`, `Target.java` |
| `Cannon`, `Cannonball` | `Cannon.java`, `Cannonball.java` |
| `CannonView` (loop, colisões, touch, sons, imersivo, diálogo) | `CannonView.java` |
| Strings / cores / layouts (Fig. 6.x) | `res/values`, `res/layout` |

### Diferenças em relação ao código do livro

Foram feitas somente adaptações mínimas para Android atual; a arquitetura e os conceitos didáticos foram mantidos.

1. **AndroidX**: `AppCompatActivity`, `androidx.fragment.app.Fragment/DialogFragment` e `FragmentContainerView` no lugar de `android.app.Fragment` e da tag `<fragment>` (obsoletos).
2. **Modo imersivo**: `setSystemUiVisibility` (obsoleto na API 30) foi trocado por `WindowInsetsControllerCompat` com o mesmo comportamento (barras ocultas que reaparecem ao deslizar).
3. **`onActivityCreated`** (obsoleto) → `onViewCreated` para configurar o controle de volume.
4. **Diálogo de fim de jogo** como classe `DialogFragment` própria (o livro usa classe anônima, que o Android não consegue recriar por reflexão).
5. **Limite de ~60 quadros/s** no game loop (`Thread.sleep`): sem isso, em aparelhos rápidos o intervalo por quadro fica tão pequeno que `(int)(velocidade × intervalo)` trunca para 0 e os elementos "travam", além de consumir 100% da CPU.
6. **Cannonball**: a borda esquerda só remove a bala quando ela se move para a esquerda (ela nasce com metade do corpo dentro do canhão, `x = -radius`).
7. **Robustez**: `lockCanvas` pode retornar `null`; o fim de jogo é tratado uma única vez; evita-se iniciar duas threads ao voltar do segundo plano; toques são sincronizados com a thread do jogo.
8. `SoundPool` com 3 streams (livro: 1) para que os sons extras não cortem o último acerto.

## Recursos extras (bônus)

- **Pontuação com combo**: 100 pts por alvo × multiplicador (até ×5) para acertos seguidos; errar ou bater no bloqueador zera o combo; bônus de tempo ao vencer.
- **Ranking local Top 5** persistido em `SharedPreferences` (`HighScoreStore`) e recorde exibido no HUD e no diálogo.
- **Efeitos visuais**: explosão de partículas nos acertos (`ParticleSystem`) e fundo em degradê.
- **Sons adicionais**: jingles de vitória e derrota e trilha sonora em loop (`MediaPlayer`, pausada/liberada conforme o ciclo de vida). Sons gerados por `tools/generate_sounds.py` (originais).
- **Localização**: textos em inglês (padrão) e português do Brasil.
- Ícone de aplicativo próprio (adaptive icon).

## Como executar

1. Instale o **Android Studio** (versão recente, ex.: Ladybug 2024.2 ou mais nova) com o SDK **Android 15 (API 35)**.
2. Clone este repositório e abra a pasta raiz (`File ▸ Open`).
3. Aguarde o **Gradle Sync** (a primeira vez baixa o Gradle 8.9 e as dependências; é necessário internet). O Android Studio cria o `local.properties` sozinho.
4. Conecte um aparelho (depuração USB) ou crie um emulador (API 26+) e clique em **Run ▶**.
5. O jogo abre em paisagem e em tela cheia.

Linha de comando (opcional): `./gradlew assembleDebug` (requer JDK 17+ e `ANDROID_HOME`).

## Tecnologias

- Java 17 (nível de linguagem do livro), Android SDK (minSdk 26, compileSdk/targetSdk 35)
- Android Gradle Plugin 8.7.3, Gradle 8.9
- AndroidX AppCompat 1.7.0, Fragment 1.8.5, Core 1.13.1
- APIs: `SurfaceView`, `SurfaceHolder`, `Canvas`, `Paint`, `SoundPool`, `MediaPlayer`, `SharedPreferences`

## Screenshots

> Adicione aqui as capturas de tela/GIF do jogo rodando (salve os arquivos em `docs/screenshots/`).

<!-- ![Jogo em execução](docs/screenshots/jogo.png) -->

## Créditos

Baseado no Cannon Game App do Capítulo 6 de *Android 6 for Programmers* (Deitel). Os sons `cannon_fire`, `target_hit` e `blocker_hit` foram fornecidos com o material da disciplina; os demais sons e o ícone são originais.
