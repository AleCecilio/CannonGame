// GameResultDialogFragment.java
// DialogFragment com o AlertDialog exibido no fim do jogo (Fig. 6.27).
// No livro é uma classe anônima dentro do CannonView; aqui é uma classe
// estática própria, pois o Android precisa recriar Fragments por reflexão.
package com.deitel.cannongame;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class GameResultDialogFragment extends DialogFragment {
   private static final String ARG_TITLE = "title";
   private static final String ARG_MESSAGE = "message";

   // implementado pelo CannonView para iniciar um novo jogo
   public interface ResetListener {
      void onResetGame();
   }

   public static GameResultDialogFragment newInstance(String title,
      String message) {
      GameResultDialogFragment fragment = new GameResultDialogFragment();
      Bundle args = new Bundle();
      args.putString(ARG_TITLE, title);
      args.putString(ARG_MESSAGE, message);
      fragment.setArguments(args);
      return fragment;
   }

   // se o sistema recriou a Activity com o diálogo aberto, o estado do jogo
   // foi perdido: descarta o diálogo (um novo jogo começa sozinho)
   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setCancelable(false); // diálogo modal
   }

   @Override
   public void onStart() {
      super.onStart();
      View cannonView = requireActivity().findViewById(R.id.cannonView);
      if (!(cannonView instanceof ResetListener) ||
         !((CannonView) cannonView).isGameOverDialogExpected()) {
         dismissAllowingStateLoss();
      }
   }

   // cria o AlertDialog e o retorna
   @NonNull
   @Override
   public Dialog onCreateDialog(Bundle savedInstanceState) {
      Bundle args = requireArguments();
      AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
      builder.setTitle(args.getString(ARG_TITLE));
      builder.setMessage(args.getString(ARG_MESSAGE));
      builder.setPositiveButton(R.string.reset_game,
         (dialog, which) -> {
            View view = requireActivity().findViewById(R.id.cannonView);
            if (view instanceof ResetListener)
               ((ResetListener) view).onResetGame(); // inicia novo jogo
         });
      return builder.create();
   }
}
