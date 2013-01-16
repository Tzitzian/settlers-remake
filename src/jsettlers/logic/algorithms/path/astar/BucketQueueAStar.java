package jsettlers.logic.algorithms.path.astar;

import java.util.BitSet;

import jsettlers.common.movable.EDirection;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.algorithms.path.IPathCalculateable;
import jsettlers.logic.algorithms.path.InvalidStartPositionException;
import jsettlers.logic.algorithms.path.Path;
import jsettlers.logic.algorithms.path.astar.normal.AbstractNewMinPriorityQueue;
import jsettlers.logic.algorithms.path.astar.normal.IAStarPathMap;
import jsettlers.logic.algorithms.path.astar.queues.bucket.MinBucketQueue;

/**
 * AStar algorithm to find paths from A to B on a hex grid
 * 
 * @author Andreas Eberle
 * 
 */
public final class BucketQueueAStar extends AbstractAStar {
	private static final byte[] xDeltaArray = EDirection.getXDeltaArray();
	private static final byte[] yDeltaArray = EDirection.getYDeltaArray();

	private final IAStarPathMap map;

	private final short height;
	private final short width;

	private final BitSet openBitSet;
	private final BitSet closedBitSet;

	final float[] costsAndHeuristics;

	final int[] depthParentHeap;

	private final AbstractNewMinPriorityQueue open;

	// float xFactor = 1.01f, yFactor = 1.02f;
	float xFactor = 1f, yFactor = 1f;

	public BucketQueueAStar(IAStarPathMap map, short width, short height) {
		this.map = map;
		this.width = width;
		this.height = height;

		this.open = new MinBucketQueue(width * height);

		this.openBitSet = new BitSet(width * height);
		this.closedBitSet = new BitSet(width * height);
		this.costsAndHeuristics = new float[width * height];

		this.depthParentHeap = new int[width * height * 3];
	}

	@Override
	public final Path findPath(IPathCalculateable requester, ShortPoint2D target) {
		ShortPoint2D pos = requester.getPos();
		return findPath(requester, pos.x, pos.y, target.x, target.y);
	}

	@Override
	public final Path findPath(IPathCalculateable requester, final short sx, final short sy, final short tx, final short ty) {
		final boolean blockedAtStart;
		if (!isInBounds(sx, sy)) {
			throw new InvalidStartPositionException("Start position is out of bounds!", sx, sy);
		} else if (!isInBounds(tx, ty) || isBlocked(requester, tx, ty) || map.getBlockedPartition(sx, sy) != map.getBlockedPartition(tx, ty)) {
			return null; // target can not be reached
		} else if (sx == tx && sy == ty) {
			return null;
		} else if (isBlocked(requester, sx, sy)) {
			blockedAtStart = true;
		} else {
			blockedAtStart = false;
		}

		float temp = xFactor; // swap the heuristic factors
		xFactor = yFactor;
		yFactor = temp;

		final int targetFlatIdx = getFlatIdx(tx, ty);

		closedBitSet.clear();
		openBitSet.clear();

		open.clear();
		boolean found = false;
		initStartNode(sx, sy, tx, ty);

		while (!open.isEmpty()) {
			int currFlatIdx = open.deleteMin();

			short x = getX(currFlatIdx);
			short y = getY(currFlatIdx);

			setClosed(x, y);

			if (targetFlatIdx == currFlatIdx) {
				found = true;
				break;
			}

			for (int i = 0; i < EDirection.NUMBER_OF_DIRECTIONS; i++) {
				short neighborX = (short) (x + xDeltaArray[i]);
				short neighborY = (short) (y + yDeltaArray[i]);

				if (isValidPosition(requester, neighborX, neighborY, blockedAtStart)) {
					int flatNeighborIdx = getFlatIdx(neighborX, neighborY);

					if (!closedBitSet.get(flatNeighborIdx)) {
						float newCosts = costsAndHeuristics[currFlatIdx] + map.getCost(x, y, neighborX, neighborY);
						if (openBitSet.get(flatNeighborIdx)) {
							final float oldCosts = costsAndHeuristics[flatNeighborIdx];

							if (oldCosts > newCosts) {
								costsAndHeuristics[flatNeighborIdx] = newCosts;
								depthParentHeap[getDepthIdx(flatNeighborIdx)] = depthParentHeap[getDepthIdx(currFlatIdx)] + 1;
								depthParentHeap[getParentIdx(flatNeighborIdx)] = currFlatIdx;

								float heuristicCosts = getHeuristicCost(neighborX, neighborY, tx, ty);
								open.increasedPriority(flatNeighborIdx, oldCosts + heuristicCosts, newCosts + heuristicCosts);
							}

						} else {
							costsAndHeuristics[flatNeighborIdx] = newCosts;
							depthParentHeap[getDepthIdx(flatNeighborIdx)] = depthParentHeap[getDepthIdx(currFlatIdx)] + 1;
							depthParentHeap[getParentIdx(flatNeighborIdx)] = currFlatIdx;
							openBitSet.set(flatNeighborIdx);
							open.insert(flatNeighborIdx, newCosts + getHeuristicCost(neighborX, neighborY, tx, ty));

							map.markAsOpen(neighborX, neighborY);
						}
					}
				}
			}
		}

		if (found) {
			int pathlength = depthParentHeap[getDepthIdx(getFlatIdx(tx, ty))];
			Path path = new Path(pathlength);

			int idx = pathlength;
			int parentFlatIdx = targetFlatIdx;

			while (idx > 0) {
				idx--;
				path.insertAt(idx, getX(parentFlatIdx), getY(parentFlatIdx));
				parentFlatIdx = depthParentHeap[getParentIdx(parentFlatIdx)];
			}

			path.initPath();

			return path;
		}

		return null;
	}

	private static final int getDepthIdx(int flatIdx) {
		return 3 * flatIdx;
	}

	private static final int getParentIdx(int flatIdx) {
		return 3 * flatIdx + 1;
	}

	static final int getHeapArrayIdx(int flatIdx) {
		return 3 * flatIdx + 2;
	}

	private final void setClosed(short x, short y) {
		closedBitSet.set(getFlatIdx(x, y));
		map.markAsClosed(x, y);
	}

	private final void initStartNode(short sx, short sy, short tx, short ty) {
		int flatIdx = getFlatIdx(sx, sy);
		depthParentHeap[getDepthIdx(flatIdx)] = 0;
		depthParentHeap[getParentIdx(flatIdx)] = -1;
		costsAndHeuristics[flatIdx] = 0;

		open.insert(flatIdx, 0 + getHeuristicCost(sx, sy, tx, ty));
		openBitSet.set(flatIdx);
	}

	private final boolean isValidPosition(IPathCalculateable requester, short x, short y, boolean blockedAtStart) {
		return isInBounds(x, y) && (!isBlocked(requester, x, y) || blockedAtStart);
	}

	private final boolean isInBounds(short x, short y) {
		return 0 <= x && x < width && 0 <= y && y < height;
	}

	private final boolean isBlocked(IPathCalculateable requester, short x, short y) {
		return map.isBlocked(requester, x, y);
	}

	private final int getFlatIdx(short x, short y) {
		return y * width + x;
	}

	private final short getX(int flatIdx) {
		return (short) (flatIdx % width);
	}

	private final short getY(int flatIdx) {
		return (short) (flatIdx / width);
	}

	private final float getHeuristicCost(final short sx, final short sy, final short tx, final short ty) {
		final float dx = (tx - sx) * xFactor;
		final float dy = (ty - sy) * yFactor;
		final float absDx = Math.abs(dx);
		final float absDy = Math.abs(dy);

		if (dx * dy > 0) { // dx and dy go in the same direction
			if (absDx > absDy) {
				return absDx;
			} else {
				return absDy;
			}
		} else {
			return absDx + absDy;
		}
	}
}
