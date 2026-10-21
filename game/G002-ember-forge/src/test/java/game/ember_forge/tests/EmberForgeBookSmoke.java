package game.ember_forge.tests;

import game.*;
import game.ember_forge.config.EmberForgeConfig;
import simulation.books.GameplayBook;
import simulation.config.SimConfig;
import simulation.engine.SimulationRunner;
import simulation.reporting.Print;
import java.nio.file.*;
import java.io.*;
import java.util.*;

public final class EmberForgeBookSmoke {
 public static void main(String[] args) throws Exception {
  var summary=new StringBuilder("mode,rounds,expected_rtp,observed_rtp,expected_trigger_probability,observed_triggers,rtp_z\n");
  for(String mode:List.of("Normal","EM1","BB1")) {
   var c=new SimConfig();c.gameId=EmberForgeConfig.ID;c.gameMode=mode;c.parWorkbookPath=EmberForgeConfig.DEFAULT_PAR;
   c.useBooks=true;c.rounds=2000;c.stake=1;c.seed=440055;c.usePreviousSeed=true;c.threads=2;c.partitions=16;c.maxSavedGameplays=10;c.exportReport=true;
   Game game=GameFactory.create(c.gameId,c.parWorkbookPath,mode);
   var book=GameplayBook.load(c,game);
   double mean=book.metadata().expectedRtp()*game.getCostMultiplier(), second=0;
   for(var e:book.entries()) {
    var round=e.value().round(); double win=round.getBaseGameResult().getWin()+round.getFreeGameResults().stream().mapToDouble(s->s.getWin()).sum();
    second+=e.weight()/(double)book.metadata().totalWeight()*win*win;
   }
   var result=new SimulationRunner(c,game).run();
   double se=Math.sqrt(second-mean*mean)/game.getCostMultiplier()/Math.sqrt(c.rounds);
   var dest=Path.of("game/G002-ember-forge/validation/books-2026-10-21",mode+"-smoke.txt");
   try(var output=new PrintWriter(Files.newBufferedWriter(dest))) {
    new Print(result,Path.of("game/G002-ember-forge/validation/runs",mode+"-book-smoke")).printToConsole(c,output);
   }
   summary.append(String.format(Locale.ROOT,"%s,%d,%.12f,%.12f,%.12f,%d,%.4f%n",mode,c.rounds,book.metadata().expectedRtp(),result.getTotalGameStats().getRtp(),
    book.metadata().triggerProbability(),result.getFreeGameStats().getFreegameTriggers(),(result.getTotalGameStats().getRtp()-book.metadata().expectedRtp())/se));
   // Paired fresh control confirms the default mechanism remains usable with the new sheets present.
   c.useBooks=false;c.exportReport=false;new SimulationRunner(c,game).run();
  }
  Files.writeString(Path.of("game/G002-ember-forge/validation/books-2026-10-21/summary.csv"),summary);
  System.out.print(summary);
 }
}
