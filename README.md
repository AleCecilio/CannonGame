# Cannon Game

Aplicação Android do jogo **Cannon Game**, desenvolvida para a disciplina de **Programação III** a partir do **Capítulo 6 – Cannon Game App** do livro *Android 6 for Programmers: An App-Driven Approach* (3ª edição, Deitel).

## Resumo

Este trabalho apresenta a implementação de um jogo 2D para a plataforma Android no qual o jogador controla um canhão fixo na borda esquerda da tela e deve destruir nove alvos móveis dentro de um tempo limitado, evitando um bloqueador que se move continuamente. A aplicação utiliza `SurfaceView` com um laço de jogo (game loop) executado em thread dedicada, renderização com `Canvas` e `Paint`, tratamento de eventos de toque, efeitos sonoros e modo imersivo. Além do escopo do capítulo de referência, o projeto incorpora um sistema de pontuação com combo, ranking local persistente, sistema de partículas, trilha sonora, modo noturno e localização em dois idiomas.

## Integrantes

- Alessandro Moreira Cecilio
- João Luis Pedrosa Teles

## Objetivo

Aplicar na prática os conceitos fundamentais de programação mobile estudados no Capítulo 6: gráficos 2D com `Canvas`/`Paint`, animação quadro a quadro, game loop em thread própria sobre `SurfaceView`/`SurfaceHolder`, eventos de toque, efeitos sonoros e modo imersivo (tela cheia).# Cannon Game

Aplicativo Android do jogo Cannon Game, desenvolvido na disciplina de Programação III do curso de Sistemas de Informação da Universidade do Estado de Minas Gerais (UEMG), Unidade Passos. O projeto é baseado no Capítulo 6 (Cannon Game App) do livro *Android 6 for Programmers: An App-Driven Approach*, 3ª edição, de Paul Deitel, Harvey Deitel e Alexander Wald.


## Descrição do jogo

O canhão está posicionado no centro da borda esquerda da tela. O jogador toca ou arrasta o dedo sobre a tela para apontar o cano na direção do toque e disparar. Apenas uma bala pode estar em cena por vez.

O objetivo é destruir os nove alvos antes que o cronômetro chegue a zero. O desempenho do jogador altera o tempo disponível:

- cada alvo atingido adiciona 3 segundos ao cronômetro;
- a colisão com o bloqueador (barra escura) desconta 2 segundos e inverte a direção horizontal da bala.

Ao final da partida, a tela exibe o resultado (vitória ou derrota), o número de tiros disparados, o tempo total, a pontuação obtida e o recorde. Um toque na tela inicia nova partida.

### Parâmetros do jogo

| Parâmetro | Valor |
|---|---|
| Tempo inicial | 10 s |
| Quantidade de alvos | 9 |
| Recompensa por alvo atingido | +3 s |
| Penalidade por colisão com o bloqueador | -2 s |
| Aumento de velocidade dos alvos restantes | +10% a cada alvo destruído |
| Pontos base por alvo | 100 |
| Multiplicador máximo de combo | x5 |
| Bônus de vitória | 10 pontos por segundo restante |
| Taxa de atualização | aproximadamente 60 quadros por segundo |
| Tamanho do ranking local | 5 pontuações |

### Controles

| Ação | Gesto |
|---|---|
| Apontar e disparar | Toque ou arraste com um dedo |
| Alternar modo dia/noturno | Toque adicional com um segundo dedo durante a partida |
| Reiniciar após o fim da partida | Toque simples na tela |

## Funcionalidades

- Canhão com mira por toque, bala, nove alvos móveis, bloqueador móvel, cronômetro, condições de vitória e derrota.
- Detecção de colisões entre bala e alvos e entre bala e bloqueador, com recompensa ou penalidade de tempo.
- Efeitos sonoros para disparo, acerto em alvo e acerto no bloqueador.
- Orientação paisagem fixa, tela cheia e modo imersivo.
- Tratamento do ciclo de vida: a thread do jogo é encerrada ao pausar a aplicação e ao destruir a `Surface`, e os recursos de áudio são liberados na destruição do fragmento.
- Interface localizada em inglês (padrão) e português do Brasil.


## Arquitetura

O código-fonte está organizado no pacote `com.deitel.cannongame`, com a seguinte distribuição de responsabilidades:

| Classe | Responsabilidade |
|---|---|
| `MainActivity` | Activity única da aplicação, responsável por exibir o fragmento principal. |
| `MainActivityFragment` | Cria o `CannonView`, repassa os eventos de ciclo de vida (`onPause`, `onResume`, `onDestroy`) e configura o controle de volume. |
| `CannonView` | Componente central. Estende `SurfaceView`, implementa `SurfaceHolder.Callback` e concentra o game loop, a detecção de colisões, o tratamento de toque, o áudio, a pontuação e a renderização. |
| `GameElement` | Classe base dos elementos retangulares do jogo (posição, velocidade vertical, desenho e som associado). |
| `Cannon` | Representa o canhão, calcula o ângulo do cano e dispara a bala. |
| `Cannonball` | Projétil com velocidades horizontal e vertical e controle de saída da tela. |
| `Target` | Alvo com movimento vertical, oscilação senoidal horizontal, fade-in, pulsação e cor própria no modo noturno. |
| `Blocker` | Obstáculo móvel que penaliza o jogador e rebate a bala. |
| `ParticleSystem` | Gerencia as partículas das explosões, com movimento radial, atrito e desvanecimento. |
| `HighScoreStore` | Persistência e consulta do ranking local. |
| `GameResultDialogFragment` | `DialogFragment` de resultado, implementado como classe própria conforme descrito nas adaptações. |



## Tecnologias e requisitos

| Item | Especificação |
|---|---|
| Linguagem | Java 17 |
| SDK Android | minSdk 26, compileSdk e targetSdk 35 |
| Android Gradle Plugin | 9.3.0 |
| Gradle | 9.6.0 (via Gradle Wrapper) |
| Bibliotecas | AndroidX AppCompat 1.7.0, Fragment 1.8.5, Core 1.13.1 |
| APIs da plataforma | `SurfaceView`, `SurfaceHolder`, `Canvas`, `Paint`, `SoundPool`, `MediaPlayer`, `SharedPreferences` |
| Ambiente de desenvolvimento | Android Studio com Android SDK 35 e JDK 17 ou superior |

O aplicativo não requer nenhuma permissão do sistema.

## Instalação e execução

### Android Studio

1. Instale o Android Studio com o SDK Android 15 (API 35).
2. Clone o repositório:
```
   git clone <URL-do-repositório>
```
3. Abra a pasta raiz do projeto em `File > Open`.
4. Aguarde a conclusão da sincronização do Gradle. Na primeira execução, o Gradle 9.6.0 e as dependências são baixados automaticamente.
5. Conecte um dispositivo com depuração USB habilitada ou crie um emulador com API 26 ou superior.
6. Execute a aplicação por meio da opção `Run`. O jogo é iniciado em orientação paisagem e em tela cheia.

### Linha de comando

Com o JDK 17 ou superior instalado e a variável de ambiente `ANDROID_HOME` configurada, é possível gerar o APK de depuração com:

```
./gradlew assembleDebug
```

O arquivo gerado é salvo em `app/build/outputs/apk/debug/`.

### Regeneração dos sons complementares (opcional)

Os arquivos `win_jingle.wav`, `lose_jingle.wav` e `bg_music.wav` são produzidos pelo script `tools/generate_sounds.py`, que depende da biblioteca `numpy`:

```
python3 tools/generate_sounds.py
```

## Créditos e referências

Os sons `cannon_fire`, `target_hit` e `blocker_hit` foram fornecidos com o material da disciplina. Os demais sons e o ícone do aplicativo são de autoria própria.

DEITEL, Paul; DEITEL, Harvey; WALD, Alexander. *Android 6 for Programmers: An App-Driven Approach*. 3. ed. Pearson, 2016.

ANDROID DEVELOPERS. *Documentação oficial da plataforma Android*. Disponível em: https://developer.android.com. Acesso em: out. 2026.

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
