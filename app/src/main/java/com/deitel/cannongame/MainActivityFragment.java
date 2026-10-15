// MainActivityFragment.java
// MainActivityFragment cria e gerencia um CannonView (Fig. 6.6)
package com.deitel.cannongame;

import android.media.AudioManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class MainActivityFragment extends Fragment {
   private CannonView cannonView; // view personalizada que exibe o jogo

   // chamado quando a view do Fragment precisa ser criada
   @Nullable
   @Override
   public View onCreateView(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
      super.onCreateView(inflater, container, savedInstanceState);

      // infla o layout fragment_main.xml
      View view = inflater.inflate(R.layout.fragment_main, container, false);

      // obtém uma referência ao CannonView
      cannonView = view.findViewById(R.id.cannonView);
      return view;
   }

   // configura o controle de volume. No livro isso é feito em
   // onActivityCreated, hoje obsoleto; onViewCreated cumpre o mesmo papel.
   @Override
   public void onViewCreated(@NonNull View view,
      @Nullable Bundle savedInstanceState) {
      super.onViewCreated(view, savedInstanceState);

      // permite que os botões de volume ajustem o volume do jogo
      requireActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);
   }

   // quando a MainActivity volta ao primeiro plano, o jogo é retomado
   // (cobre o caso de pausa sem destruição da Surface, ex.: multi-janela)
   @Override
   public void onResume() {
      super.onResume();
      cannonView.resumeGame();
   }

   // quando a MainActivity é pausada, termina o game loop
   @Override
   public void onPause() {
      super.onPause();
      cannonView.stopGame(); // termina o jogo
   }

   // quando a MainActivity é destruída, o Fragment libera os recursos
   @Override
   public void onDestroy() {
      super.onDestroy();
      cannonView.releaseResources();
   }
}
