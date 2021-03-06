/*******************************************************************************
 * Copyright (c) 2015
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *******************************************************************************/
package jsettlers.algorithms.partitions;

import static org.junit.Assert.assertEquals;

import java.util.BitSet;

import org.junit.Test;

public class PartitionsCalculatorAlgorithmTest {

	private static final int HEIGHT = 100;
	private static final int WIDTH = 100;

	@Test
	public void testSqaureWithBlockingLine() {
		BitSet containing = new BitSet(HEIGHT * WIDTH);
		for (int y = 0; y < HEIGHT; y++) {
			for (int x = 0; x < WIDTH; x++) {
				if (x > 0 && y > 0 && x < WIDTH - 1 && y < HEIGHT - 1)
					containing.set(x + y * WIDTH);
			}
		}

		final IBlockingProvider blockingProvider = new IBlockingProvider() {
			@Override
			public boolean isBlocked(int x, int y) {
				return x == y;
			}
		};

		final PartitionCalculatorAlgorithm algo = new PartitionCalculatorAlgorithm(0, 0, WIDTH, HEIGHT, containing, blockingProvider);
		algo.calculatePartitions();

		// visualizeAlgoResult(blockingProvider, algo);

		assertEquals(PartitionCalculatorAlgorithm.NUMBER_OF_RESERVED_PARTITIONS + 2, algo.getNumberOfPartitions());
	}

	// @SuppressWarnings("unused")
	// private void visualizeAlgoResult(final IBlockingProvider blockingProvider, final PartitionCalculatorAlgorithm algo) {
	// TestUtils.openTestWindow(new GraphicsGridAdapter(WIDTH, HEIGHT) {
	// @Override
	// public int getDebugColorAt(int x, int y, EDebugColorModes debugColorMode) {
	// int value;
	//
	// value = algo.getPartitionAt(x, y) + 1;
	//
	// // boolean isBlocked = blockingProvider.isBlocked(x, y);
	// boolean isBlocked = false;
	//
	// return isBlocked ? Color.BLACK.getABGR() : Color
	// .getARGB((value % 3) * 0.33f, ((value / 3) % 3) * 0.33f, ((value / 9) % 3) * 0.33f, 1);
	// }
	// });
	//
	// try {
	// Thread.sleep(60000);
	// } catch (InterruptedException e) {
	// e.printStackTrace();
	// }
	// }
}
