package uk.ac.bris.cs.scotlandyard.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static uk.ac.bris.cs.scotlandyard.model.Colour.Black;
import static uk.ac.bris.cs.scotlandyard.model.Colour.Blue;
import static uk.ac.bris.cs.scotlandyard.model.Colour.Green;
import static uk.ac.bris.cs.scotlandyard.model.Colour.Red;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.Bus;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.Secret;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.Taxi;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.Underground;

import java.util.Set;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import uk.ac.bris.cs.scotlandyard.model.PlayerConfiguration.Builder;

/**
 * Tests whether valid moves are generated by the model
 */
@SuppressWarnings("unchecked")
public class ModelValidMoveTest extends ModelTestBase {

	// -- Detective related tests --

	@Test
	public void testDetectiveAt128MovesShouldProduce13ValidMoves() {
		PlayerConfiguration black = of(Black, 104);
		PlayerConfiguration blue = new Builder(Blue).using(mocked())
				.with(makeTickets(11, 8, 4, 0, 0))
				.at(128).build();
		doAnswer(tryChoose(ticket(Black, Taxi, 86)))
				.when(black.player).makeMove(any(), anyInt(), anySet(), any());
		createGame(black, blue).startRotate();
		ArgumentCaptor<Set<Move>> movesCaptor = ArgumentCaptor.forClass(Set.class);
		// capture the moves supplied by the model
		verify(blue.player).makeMove(any(), anyInt(), movesCaptor.capture(), any());
		assertThat(movesCaptor.getValue()).containsOnly(
				ticket(Blue, Underground, 89),
				ticket(Blue, Underground, 185),
				ticket(Blue, Underground, 140),
				ticket(Blue, Bus, 187),
				ticket(Blue, Bus, 199),
				ticket(Blue, Bus, 135),
				ticket(Blue, Bus, 142),
				ticket(Blue, Bus, 161),
				ticket(Blue, Taxi, 188),
				ticket(Blue, Taxi, 142),
				ticket(Blue, Taxi, 143),
				ticket(Blue, Taxi, 160),
				ticket(Blue, Taxi, 172));
	}

	@Test
	public void testDetectiveMovesOmittedIfNotEnoughTickets() {
		PlayerConfiguration black = of(Black, 104);
		PlayerConfiguration blue = new Builder(Blue).using(mocked())
				.with(makeTickets(0, 8, 4, 0, 0))
				.at(128).build();
		doAnswer(tryChoose(ticket(Black, Taxi, 86)))
				.when(black.player).makeMove(any(), anyInt(), anySet(), any());
		createGame(black, blue).startRotate();
		ArgumentCaptor<Set<Move>> movesCaptor = ArgumentCaptor.forClass(Set.class);
		// capture the moves supplied by the model
		verify(blue.player).makeMove(any(), anyInt(), movesCaptor.capture(), any());
		// no taxi moves because of insufficient tickets
		assertThat(movesCaptor.getValue()).containsOnly(
				ticket(Blue, Underground, 89),
				ticket(Blue, Underground, 185),
				ticket(Blue, Underground, 140),
				ticket(Blue, Bus, 187),
				ticket(Blue, Bus, 199),
				ticket(Blue, Bus, 135),
				ticket(Blue, Bus, 142),
				ticket(Blue, Bus, 161));
	}

	@Test
	public void testDetectiveWithNoValidMovesShouldProducePassMove() {
		PlayerConfiguration black = of(Black, 104);
		PlayerConfiguration red = of(Red, 111);
		PlayerConfiguration blue = new Builder(Blue).using(mocked())
				.with(makeTickets(0, 0, 0, 0, 0))
				.at(128).build();
		doAnswer(tryChoose(ticket(Black, Taxi, 86)))
				.when(black.player).makeMove(any(), anyInt(), anySet(), any());
		doAnswer(tryChoose(ticket(Red, Taxi, 124)))
				.when(red.player).makeMove(any(), anyInt(), anySet(), any());
		createGame(black, red, blue).startRotate();
		ArgumentCaptor<Set<Move>> movesCaptor = ArgumentCaptor.forClass(Set.class);
		verify(blue.player).makeMove(any(), anyInt(), movesCaptor.capture(), any());
		// blue has no tickets, he does not have any valid move; he receives a
		// pass move
		assertThat(movesCaptor.getValue()).containsOnly(
				pass(Blue));
	}

	@Test
	public void testDetectiveMoveOmittedIfLocationOccupiedByOtherDetective() {
		// this happens around london zoo where an awkward taxi route appears
		// around location 2
		PlayerConfiguration black = of(Black, 104);

		PlayerConfiguration red = new Builder(Red).using(mocked())
				.with(noTickets())
				.at(10).build();
		PlayerConfiguration green = of(Green, 2);

		doAnswer(tryChoose(ticket(Black, Taxi, 86)))
				.when(black.player).makeMove(any(), anyInt(), anySet(), any());
		// red does not move
		doAnswer(tryChoose(pass(Red)))
				.when(red.player).makeMove(any(), anyInt(), anySet(), any());
		// green can only go the other way
		doAnswer(chooseFirst())
				.when(green.player).makeMove(any(), anyInt(), anySet(), any());

		createGame(black, red, green)
				.startRotate();
		ArgumentCaptor<Set<Move>> movesCaptor = ArgumentCaptor.forClass(Set.class);
		verify(green.player).makeMove(any(), anyInt(), movesCaptor.capture(), any());
		// green can only move to 20 because 10 is blocked
		assertThat(movesCaptor.getValue()).containsOnly(
				ticket(Green, Taxi, 20));
	}

	@Test
	public void testDetectiveMoveNotOmittedIfDestinationOccupiedByMrX() {
		PlayerConfiguration black = of(Black, 86);
		PlayerConfiguration blue = of(Blue, 85);

		doAnswer(tryChoose(ticket(Black, Taxi, 103)))
				.when(black.player).makeMove(any(), anyInt(), anySet(), any());
		doAnswer(chooseFirst())
				.when(blue.player).makeMove(any(), anyInt(), anySet(), any());
		createGame(black, blue).startRotate();
		ArgumentCaptor<Set<Move>> movesCaptor = ArgumentCaptor.forClass(Set.class);
		verify(blue.player).makeMove(any(), anyInt(), movesCaptor.capture(), any());
		// MrX's location should be a valid destination, where he will be caught
		assertThat(movesCaptor.getValue()).containsOnly(
				ticket(Blue, Taxi, 103),
				ticket(Blue, Taxi, 68),
				ticket(Blue, Taxi, 84));
	}

	// MrX related tests

	@Test
	public void testMrXDoubleMoveIntermediateMovesOmittedIfDestinationOccupiedByDetectives() {
		PlayerConfiguration black = new Builder(Black).using(mocked())
				.with(makeTickets(4, 3, 3, 2, 5))
				.at(104).build();
		createGame(black, of(Blue, 116)).startRotate();
		ArgumentCaptor<Set<Move>> movesCaptor = ArgumentCaptor.forClass(Set.class);
		verify(black.player).makeMove(any(), anyInt(), movesCaptor.capture(), any());
		// no destination should end up at 116(blue), applies for first move and
		// second move
		assertThat(movesCaptor.getValue()).containsOnly(
				ticket(Black, Taxi, 86),
				ticket(Black, Secret, 86),
				x2(Black, Taxi, 86, Bus, 52),
				x2(Black, Taxi, 86, Secret, 52),
				x2(Black, Taxi, 86, Taxi, 69),
				x2(Black, Taxi, 86, Secret, 69),
				x2(Black, Taxi, 86, Bus, 87),
				x2(Black, Taxi, 86, Secret, 87),
				x2(Black, Taxi, 86, Bus, 102),
				x2(Black, Taxi, 86, Secret, 102),
				x2(Black, Taxi, 86, Taxi, 103),
				x2(Black, Taxi, 86, Secret, 103),
				x2(Black, Taxi, 86, Taxi, 104),
				x2(Black, Taxi, 86, Secret, 104),
				x2(Black, Secret, 86, Bus, 52),
				x2(Black, Secret, 86, Secret, 52),
				x2(Black, Secret, 86, Taxi, 69),
				x2(Black, Secret, 86, Secret, 69),
				x2(Black, Secret, 86, Bus, 87),
				x2(Black, Secret, 86, Secret, 87),
				x2(Black, Secret, 86, Bus, 102),
				x2(Black, Secret, 86, Secret, 102),
				x2(Black, Secret, 86, Taxi, 103),
				x2(Black, Secret, 86, Secret, 103),
				x2(Black, Secret, 86, Taxi, 104),
				x2(Black, Secret, 86, Secret, 104));
	}

	@Test
	public void testMrXMovesOmittedIfDestinationOccupiedByDetectives() {
		PlayerConfiguration black = new Builder(Black).using(mocked())
				.with(makeTickets(4, 3, 3, 0, 5))
				.at(104).build();
		createGame(black, of(Blue, 116)).startRotate();
		ArgumentCaptor<Set<Move>> movesCaptor = ArgumentCaptor.forClass(Set.class);
		verify(black.player).makeMove(any(), anyInt(), movesCaptor.capture(), any());
		// no destination should end up at 116(blue) and no double move
		assertThat(movesCaptor.getValue()).containsOnly(
				ticket(Black, Taxi, 86),
				ticket(Black, Secret, 86));
	}

	@Test
	public void testMrXMustHaveEnoughTicketsForDoubleMove() {
		PlayerConfiguration black = new Builder(Black).using(mocked())
				.with(makeTickets(1, 1, 0, 2, 0))
				.at(104).build();
		createGame(black, of(Blue, 117)).startRotate();
		ArgumentCaptor<Set<Move>> movesCaptor = ArgumentCaptor.forClass(Set.class);
		verify(black.player).makeMove(any(), anyInt(), movesCaptor.capture(), any());
		// no repeated tickets with double move for taxi and bus because we only
		// have one of each
		assertThat(movesCaptor.getValue()).containsOnly(
				ticket(Black, Taxi, 86),
				ticket(Black, Taxi, 116),
				x2(Black, Taxi, 86, Bus, 52),
				x2(Black, Taxi, 86, Bus, 87),
				x2(Black, Taxi, 86, Bus, 102),
				x2(Black, Taxi, 86, Bus, 116),
				x2(Black, Taxi, 116, Bus, 86),
				x2(Black, Taxi, 116, Bus, 108),
				x2(Black, Taxi, 116, Bus, 127),
				x2(Black, Taxi, 116, Bus, 142));
	}

	@Test
	public void testMrXNoSecretMovesIfNoSecretMoveTickets() {
		PlayerConfiguration black = new Builder(Black).using(mocked())
				.with(makeTickets(4, 3, 3, 2, 0))
				.at(104).build();
		createGame(black, of(Blue, 117)).startRotate();
		ArgumentCaptor<Set<Move>> movesCaptor = ArgumentCaptor.forClass(Set.class);
		verify(black.player).makeMove(any(), anyInt(), movesCaptor.capture(), any());
		// no secret move generated if no secret move tickets
		assertThat(movesCaptor.getValue()).containsOnly(
				ticket(Black, Taxi, 86),
				ticket(Black, Taxi, 116),
				x2(Black, Taxi, 86, Bus, 52),
				x2(Black, Taxi, 86, Taxi, 69),
				x2(Black, Taxi, 86, Bus, 87),
				x2(Black, Taxi, 86, Bus, 102),
				x2(Black, Taxi, 86, Taxi, 103),
				x2(Black, Taxi, 86, Taxi, 104),
				x2(Black, Taxi, 86, Bus, 116),
				x2(Black, Taxi, 116, Bus, 86),
				x2(Black, Taxi, 116, Taxi, 104),
				x2(Black, Taxi, 116, Bus, 108),
				x2(Black, Taxi, 116, Taxi, 118),
				x2(Black, Taxi, 116, Taxi, 127),
				x2(Black, Taxi, 116, Bus, 127),
				x2(Black, Taxi, 116, Bus, 142));
	}

	@Test
	public void testMrXNoDoubleMovesIfNoDoubleMoveTickets() {
		PlayerConfiguration black = new Builder(Black).using(mocked())
				.with(makeTickets(4, 3, 3, 0, 5))
				.at(104).build();
		createGame(black, of(Blue, 117)).startRotate();
		ArgumentCaptor<Set<Move>> movesCaptor = ArgumentCaptor.forClass(Set.class);
		verify(black.player).makeMove(any(), anyInt(), movesCaptor.capture(), any());
		// no double move generated if no double move tickets
		assertThat(movesCaptor.getValue()).containsOnly(
				ticket(Black, Taxi, 86),
				ticket(Black, Secret, 86),
				ticket(Black, Taxi, 116),
				ticket(Black, Secret, 116));
	}

	@Test
	public void testMrXNoDoubleMovesIfNotEnoughRoundLeft() {
		PlayerConfiguration black = new Builder(Black).using(mocked())
				.with(makeTickets(4, 3, 3, 2, 5))
				.at(104).build();
		// we have one round only
		createGame(rounds(true), black, of(Blue, 117)).startRotate();
		ArgumentCaptor<Set<Move>> movesCaptor = ArgumentCaptor.forClass(Set.class);
		verify(black.player).makeMove(any(), anyInt(), movesCaptor.capture(), any());
		// no double move because we have no next round to play the second move
		assertThat(movesCaptor.getValue()).containsOnly(
				ticket(Black, Taxi, 86),
				ticket(Black, Secret, 86),
				ticket(Black, Taxi, 116),
				ticket(Black, Secret, 116));
	}

	@Test
	public void testMrXNoTicketMovesIfNoTicketMoveTickets() {
		PlayerConfiguration black = new Builder(Black).using(mocked())
				.with(makeTickets(1, 0, 1, 0, 0))
				.at(104).build();
		createGame(rounds(true), black, of(Blue, 117)).startRotate();
		ArgumentCaptor<Set<Move>> movesCaptor = ArgumentCaptor.forClass(Set.class);
		verify(black.player).makeMove(any(), anyInt(), movesCaptor.capture(), any());
		// no double move generated if no double move tickets
		assertThat(movesCaptor.getValue()).containsOnly(
				new TicketMove(Black, Taxi, 86),
				new TicketMove(Black, Taxi, 116));
	}

	@Test
	public void testMrXAt104ShouldProduce60ValidMoves() {
		PlayerConfiguration black = new Builder(Black).using(mocked())
				.with(makeTickets(4, 3, 3, 2, 5))
				.at(104).build();
		createGame(black, of(Blue, 117)).startRotate();
		ArgumentCaptor<Set<Move>> movesCaptor = ArgumentCaptor.forClass(Set.class);
		verify(black.player).makeMove(any(), anyInt(), movesCaptor.capture(), any());
		// 60 moves in total, note the permutation pattern and relation of
		// DoubleMove to TicketMove
		assertThat(movesCaptor.getValue()).containsOnly(
				ticket(Black, Taxi, 86),
				ticket(Black, Secret, 86),
				ticket(Black, Taxi, 116),
				ticket(Black, Secret, 116),
				x2(Black, Taxi, 86, Bus, 52),
				x2(Black, Taxi, 86, Secret, 52),
				x2(Black, Taxi, 86, Taxi, 69),
				x2(Black, Taxi, 86, Secret, 69),
				x2(Black, Taxi, 86, Bus, 87),
				x2(Black, Taxi, 86, Secret, 87),
				x2(Black, Taxi, 86, Bus, 102),
				x2(Black, Taxi, 86, Secret, 102),
				x2(Black, Taxi, 86, Taxi, 103),
				x2(Black, Taxi, 86, Secret, 103),
				x2(Black, Taxi, 86, Taxi, 104),
				x2(Black, Taxi, 86, Secret, 104),
				x2(Black, Taxi, 86, Bus, 116),
				x2(Black, Taxi, 86, Secret, 116),
				x2(Black, Secret, 86, Bus, 52),
				x2(Black, Secret, 86, Secret, 52),
				x2(Black, Secret, 86, Taxi, 69),
				x2(Black, Secret, 86, Secret, 69),
				x2(Black, Secret, 86, Bus, 87),
				x2(Black, Secret, 86, Secret, 87),
				x2(Black, Secret, 86, Bus, 102),
				x2(Black, Secret, 86, Secret, 102),
				x2(Black, Secret, 86, Taxi, 103),
				x2(Black, Secret, 86, Secret, 103),
				x2(Black, Secret, 86, Taxi, 104),
				x2(Black, Secret, 86, Secret, 104),
				x2(Black, Secret, 86, Bus, 116),
				x2(Black, Secret, 86, Secret, 116),
				x2(Black, Taxi, 116, Bus, 86),
				x2(Black, Taxi, 116, Secret, 86),
				x2(Black, Taxi, 116, Taxi, 104),
				x2(Black, Taxi, 116, Secret, 104),
				x2(Black, Taxi, 116, Bus, 108),
				x2(Black, Taxi, 116, Secret, 108),
				x2(Black, Taxi, 116, Taxi, 118),
				x2(Black, Taxi, 116, Secret, 118),
				x2(Black, Taxi, 116, Taxi, 127),
				x2(Black, Taxi, 116, Secret, 127),
				x2(Black, Taxi, 116, Bus, 127),
				x2(Black, Taxi, 116, Secret, 127),
				x2(Black, Taxi, 116, Bus, 142),
				x2(Black, Taxi, 116, Secret, 142),
				x2(Black, Secret, 116, Bus, 86),
				x2(Black, Secret, 116, Secret, 86),
				x2(Black, Secret, 116, Taxi, 104),
				x2(Black, Secret, 116, Secret, 104),
				x2(Black, Secret, 116, Bus, 108),
				x2(Black, Secret, 116, Secret, 108),
				x2(Black, Secret, 116, Taxi, 118),
				x2(Black, Secret, 116, Secret, 118),
				x2(Black, Secret, 116, Taxi, 127),
				x2(Black, Secret, 116, Secret, 127),
				x2(Black, Secret, 116, Bus, 127),
				x2(Black, Secret, 116, Secret, 127),
				x2(Black, Secret, 116, Bus, 142),
				x2(Black, Secret, 116, Secret, 142));
	}

}
