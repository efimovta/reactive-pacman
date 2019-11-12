package qlearn;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.lang.*;

import org.coinen.reactive.pacman.agent.core.G;
import org.coinen.reactive.pacman.agent.core.Game;
import org.coinen.reactive.pacman.agent.model.GameState;
import org.coinen.pacman.learning.CaseStudy;
import org.coinen.reactive.pacman.agent.model.Knowledge;
import org.coinen.reactive.pacman.agent.service.utils.GameUtils;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.FluxProcessor;


/*
 * This class implements whole functional Q-learning environment. Its methods are used by PacMan simulator to make the bot learn
 * from experience in order to improve its performance.
 * */
public class Q_learn {
    /**
     * Q-Learning attributes.
     * If 'learning' is true, the program will simulate an amount of 'n_ep' of runs, updating learning data.
     */

    public static FluxProcessor<CaseStudy, CaseStudy> caseStudyFluxProcessor = DirectProcessor.create();

    public static boolean learning = true;                                                    // True if program learns from each action
    public static boolean load = true;                                                        // True if PacMan start with information from previous experiments
    public static boolean update = true;                                                    // True if current learning is updated during next simulations
    public static int n_ep = 100;                                                            // Number of simulation episodes

    public static ArrayList<ArrayList<Float>> Q = new ArrayList<>();                        // Action-state matrix
    public static List<GameState> S = new ArrayList<>();                            // Set of different states

    public static ArrayList<ArrayList<Float>> currQ = new ArrayList<>();        // Action-state matrix

    public static int prevScore;
    public static int prevTime;
    public static int reward;                                                                // Next reward
    public static int totalReward;                                                            // Total reward earned while being in some state
    public static boolean eaten = false, justEaten = false;                                                    // Used for testing negative rewards

    // Q-learning parameters
    public static final float alpha = 0.2f;                                                    // Alpha is the learning rate: how much the newer information will override the previous one
    public static final float gamma = 0.8f;                                                    // Gamma determines the importance of future rewards.
    public static float eps = 0f;                                                            // Eps. determines how likely is doing a random movement (exploring)

    // Case-based reasoning parameters
    public static float w1 = 0.85f, w2 = 0f, w3 = 0.05f, w4 = 0.05f, w5 = 0.05f;                        // Weights: w1:pills, w2:ghosts, w3:power pills, w4:edible ghosts, w5: intersections
    public static final int MAX_CASES = Integer.MAX_VALUE;                                                // Max. number of cases allowed in case base
    public static final int CASES_TO_DELETE = 10000;                                                    // Number of cases to delete when case base is full.
    public static final float newCaseThreshold = 0.025f;                                                // Case threshold used to distinguish two cases.
    public static final int MAX_MAP_DISTANCE = GameUtils.DISCRETE_FAR;
    public static final int MAX_STATE_DISTANCE = 4;
    public static float currSimilarity = 0;
    public static float comp = MAX_STATE_DISTANCE;
    public static float currBest = MAX_STATE_DISTANCE;


    // Game states
    public static GameState initState;                                                        // Initial state of the game
    public static GameState currState;                                                        // Current state of the game
    public static GameState nextState;                                                        // Next state of the game
    public static GameState prevState;
    public static GameState eatenState;
    public static int prevAction = Game.INITIAL_PAC_DIR;
    public static int stateIndex = -1;                                                        // Used to calculate the index that next state will have
    public static int currAction = 0;                                                            // PacMan direction
    // Chosen PacMan direction: 0 Up, 1 Right, 2 Down, 3 Left
    public static boolean initialized = false;                                                // Used to avoid adding initial state to the list each time a new game starts


    // Data used to draw plots
    public static List<Integer> scores = new ArrayList<>();                            // Scores reached during the simulation
    public static List<Integer> drawableScores = new ArrayList<>();                    // Values that will appear in plots (after computing means with 'scores' list values)
    public static List<Integer> rewards = new ArrayList<>();                            // Rewards reached during the simulation
    public static List<Integer> finalScores = new ArrayList<>();
    public static List<Integer> times = new ArrayList<>();
    public static List<Integer> drawableTimes = new ArrayList<>();

    public static List<Integer> randomScores = new ArrayList<>();
    public static List<Integer> drawableRandomScores = new ArrayList<>();

    public static List<Integer> closestPillScores = new ArrayList<>();
    public static List<Integer> drawableClosestPillScores = new ArrayList<>();

    public static List<Integer> numberOfCases = new ArrayList<>();                    // Number of cases in each episode

    public static List<Integer> retrievedCases = new ArrayList<>();                    // Number of retrieved cases each stage
    public static int currRetrieved = 0;

    public static List<Integer> stateCounter = new ArrayList<>();                    // Number of times each case is retrieved
    public static List<Integer> addedCases = new ArrayList<>();                        // New cases added in each episode
    public static List<Float> similarities = new ArrayList<>();                        // Similarities of retrieved cases in a stage
    public static List<Float> averageSimilarities = new ArrayList<>();                // Average similarities of retrieved cases in a simulation
    public static int currAdded = 0;

    // Stats
    public static List<Integer> levelsCompleted = new ArrayList<>();                // Average similarities of retrieved cases in a simulation
    public static int currLevels = 0;
    public static float avgLevels;
    public static int maxScore, maxTime, maxLevel;
    public static float avgScore, avgTime;
    public static float stdScore, stdTime;

    // Variables used for threaded searching
    public static int searchResult;
    public static float r1, r2, s1, s2;

    /*
     *  This lists will store precalculated information about distances
     *  */
    public static ArrayList<ArrayList<Integer>> currDistancesUp = new ArrayList<>();
    public static ArrayList<ArrayList<Integer>> currDistancesRight = new ArrayList<>();
    public static ArrayList<ArrayList<Integer>> currDistancesDown = new ArrayList<>();
    public static ArrayList<ArrayList<Integer>> currDistancesLeft = new ArrayList<>();

    public static ArrayList<ArrayList<Integer>> distances = new ArrayList<>();
    public static ArrayList<ArrayList<Integer>> distancesUp = new ArrayList<>();
    public static ArrayList<ArrayList<Integer>> distancesRight = new ArrayList<>();
    public static ArrayList<ArrayList<Integer>> distancesDown = new ArrayList<>();
    public static ArrayList<ArrayList<Integer>> distancesLeft = new ArrayList<>();

    public static ArrayList<ArrayList<Integer>> distances2Up = new ArrayList<>();
    public static ArrayList<ArrayList<Integer>> distances2Right = new ArrayList<>();
    public static ArrayList<ArrayList<Integer>> distances2Down = new ArrayList<>();
    public static ArrayList<ArrayList<Integer>> distances2Left = new ArrayList<>();

    public static ArrayList<ArrayList<Integer>> distances3Up = new ArrayList<>();
    public static ArrayList<ArrayList<Integer>> distances3Right = new ArrayList<>();
    public static ArrayList<ArrayList<Integer>> distances3Down = new ArrayList<>();
    public static ArrayList<ArrayList<Integer>> distances3Left = new ArrayList<>();

    public static ArrayList<ArrayList<Integer>> distances4Up = new ArrayList<>();
    public static ArrayList<ArrayList<Integer>> distances4Right = new ArrayList<>();
    public static ArrayList<ArrayList<Integer>> distances4Down = new ArrayList<>();
    public static ArrayList<ArrayList<Integer>> distances4Left = new ArrayList<>();

    /**
     * Sets initial state and initializes state list and Q matrix
     */
    public synchronized static void initialize(Game game) {

        // Different state/case representations are commented
        if (!initialized) {
            /* For behavioral agent*/
            //initState = new GameState(0, game.getPathDistance(game.getCurPacManLoc(), GameState.getNearestPill(game)), game.getPathDistance(game.getCurPacManLoc(), GameState.getNearestPowerPill(game)), GameState.nearGhosts(game), GameState.edibleGhost(game));

//            CountDownLatch latch = new CountDownLatch(4);
//            System.out.println("Started calculation");
//            new Thread(() -> {
//                try {
//
//                    calculateDistancesInDirection(game, Direction.UP.index);
//                } catch (Throwable t) {
//                    System.out.println("aaaaa it died " + t);
//                }
//                latch.countDown();
//            }).start();
//            new Thread(() -> {
//                try {
//                    calculateDistancesInDirection(game, Direction.DOWN.index);
//                } catch (Throwable t) {
//                    System.out.println("aaaaa it died " + t);
//                }
//                latch.countDown();
//            }).start();
//            new Thread(() -> {
//                try {
//                    calculateDistancesInDirection(game, Direction.RIGHT.index);
//                } catch (Throwable t) {
//                    System.out.println("aaaaa it died " + t);
//                }
//                latch.countDown();
//            }).start();
//            new Thread(() -> {
//                try {
//                    calculateDistancesInDirection(game, Direction.LEFT.index);
//                } catch (Throwable t) {
//                    System.out.println("aaaaa it died " + t);
//                }
//                latch.countDown();
//            }).start();
//
//            try {
//                latch.await();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//
//            System.out.println("Finished calculation");
            prevAction = game.getPossiblePacManDirs()[0];
            initState = new GameState(GameUtils.getNextPill(game, Game.UP), GameUtils.getNextPill(game, Game.RIGHT), GameUtils.getNextPill(game, Game.DOWN), GameUtils.getNextPill(game, Game.LEFT),
                    GameUtils.getNextGhost(game, Game.UP), GameUtils.getNextGhost(game, Game.RIGHT), GameUtils.getNextGhost(game, Game.DOWN), GameUtils.getNextGhost(game, Game.LEFT),
                    GameUtils.getNextPowerPill(game, Game.UP), GameUtils.getNextPowerPill(game, Game.RIGHT), GameUtils.getNextPowerPill(game, Game.DOWN), GameUtils.getNextPowerPill(game, Game.LEFT),
                    GameUtils.getNextEdibleGhost(game, Game.UP), GameUtils.getNextEdibleGhost(game, Game.RIGHT), GameUtils.getNextEdibleGhost(game, Game.DOWN), GameUtils.getNextEdibleGhost(game, Game.LEFT),
                    GameUtils.getNextIntersection(game, Game.UP), GameUtils.getNextIntersection(game, Game.RIGHT), GameUtils.getNextIntersection(game, Game.DOWN), GameUtils.getNextIntersection(game, Game.LEFT));


			/*
			initState = new GameState(0, GameState.getNextPill(game,Game.UP), GameState.getNextPill(game,Game.RIGHT),GameState.getNextPill(game,Game.DOWN),GameState.getNextPill(game,Game.LEFT),
					GameState.closeGhostInDirection(game, Game.UP), GameState.closeGhostInDirection(game, Game.RIGHT), GameState.closeGhostInDirection(game, Game.DOWN), GameState.closeGhostInDirection(game, Game.LEFT),
					GameState.getNextPowerPill(game, Game.UP), GameState.getNextPowerPill(game, Game.RIGHT),GameState.getNextPowerPill(game, Game.DOWN),GameState.getNextPowerPill(game, Game.LEFT),
					GameState.getNextEdibleGhost(game, Game.UP), GameState.getNextEdibleGhost(game, Game.RIGHT),GameState.getNextEdibleGhost(game, Game.DOWN),GameState.getNextEdibleGhost(game, Game.LEFT),
					GameState.getNextIntersection(game, Game.UP), GameState.getNextIntersection(game, Game.RIGHT),GameState.getNextIntersection(game, Game.DOWN),GameState.getNextIntersection(game, Game.LEFT));
			*/
            //eatenState= new GameState(-1, -1, -1, -1, -1);

            prevScore = 0;
            reward = 0;
            totalReward = 0;

            stateCounter.add(1);
            S.add(initState);

            Random rnd = new Random();
            float f1 = rnd.nextFloat(), f2 = rnd.nextFloat(), f3 = rnd.nextFloat(), f4 = rnd.nextFloat();
            Q.add(new ArrayList<>(Arrays.asList(f1, f2, f3, f4)));
            currQ.add(new ArrayList<>(Arrays.asList(f1, f2, f3, f4)));

            initialized = true;
            currState = initState;
            prevState = currState;
        }
        currState = initState;

    }

    // Max. function with 4 elements
    public static float max(float a, float b, float c, float d) {
        return Math.max(Math.max(a, b), Math.max(c, d));
    }

    // Min. function with 4 elements
    public static float min(float a, float b, float c, float d) {
        return Math.min(Math.min(a, b), Math.min(c, d));
    }

    // Returns the maximum value in 'index'th row of Q matrix
    public static float maxQRow(int index) {
        return Collections.max(currQ.get(index));
    }

    public static float maxQRow(org.coinen.reactive.pacman.agent.model.CaseStudy caseStudy) {
        return Math.max(Math.max(Math.max(caseStudy.upWeight, caseStudy.rightWeight), caseStudy.downWeight), caseStudy.leftWeight);
    }

    // Returns the index of maximum value in 'index'th row of Q matrix
    public static int maxQRowIndex(int index) {
        if (max(Q.get(index).get(0), Q.get(index).get(1), Q.get(index).get(2), Q.get(index).get(3)) == Q.get(index).get(0)) {
            return 0;
        } else if (max(Q.get(index).get(0), Q.get(index).get(1), Q.get(index).get(2), Q.get(index).get(3)) == Q.get(index).get(1)) {
            return 1;
        } else if (max(Q.get(index).get(0), Q.get(index).get(1), Q.get(index).get(2), Q.get(index).get(3)) == Q.get(index).get(2)) {
            return 2;
        } else if (max(Q.get(index).get(0), Q.get(index).get(1), Q.get(index).get(2), Q.get(index).get(3)) == Q.get(index).get(3)) {
            return 3;
        } else return 0;
    }


    public static boolean isExplored(int index) {
		/*for(int i=0; i<4; i++){
			if(Q.get(index).get(i) != 0) return true;
		}*/
        if (stateCounter.get(index) > 2) return true;

        return false;
    }

    /*
     * Returns i such as Q[index,i] is the maximum value in that row and i is in directions[].
     *
     * @directions: Possible directions for PacMan
     * @index: State index
     *
     * */
    public static int maxQRowDirIndex(int[] directions, int index) {
        float max = Q.get(index).get(directions[0]);
        int res = directions[0];

        for (int i = 1; i < directions.length; i++) {
            if (Q.get(index).get(directions[i]) > max) {
                max = Q.get(index).get(directions[i]);
                res = directions[i];
            }
        }

        return res;
    }

    /*
     * Returns i such as Q[index,i] is the maximum value in that row and i is in directions[].
     *
     * @directions: Possible directions for PacMan
     * @index: State index
     *
     * */
    public static int maxQRowDirIndex(int[] directions, Knowledge outcome) {
        var caseStudy = outcome.caseStudy;
        float max = resolveWeightFromCaseStudy(caseStudy, directions[0]);
        int bestDirection = directions[0];

        for (int i = 1; i < directions.length; i++) {
            int possibleBestDirection = directions[i];
            float possibleMax = resolveWeightFromCaseStudy(caseStudy, possibleBestDirection);
            if (possibleMax > max) {
                max = possibleMax;
                bestDirection = possibleBestDirection;
            }
        }

        return bestDirection;
    }

    public static float resolveWeightFromCaseStudy(org.coinen.reactive.pacman.agent.model.CaseStudy caseStudy, int direction) {
        switch (direction) {
            case 0:
                return caseStudy.upWeight;
            case 1:
                return caseStudy.rightWeight;
            case 2:
                return caseStudy.downWeight;
            case 3:
                return caseStudy.leftWeight;
        }

        throw new IllegalArgumentException("Unsupported Direction");
    }

    public static org.coinen.reactive.pacman.agent.model.CaseStudy updateWeightForCaseStudy(org.coinen.reactive.pacman.agent.model.CaseStudy caseStudy, int direction, float weight) {
        switch (direction) {
            case 0:
                return new org.coinen.reactive.pacman.agent.model.CaseStudy(weight, caseStudy.rightWeight, caseStudy.downWeight, caseStudy.leftWeight);
            case 1:
                return new org.coinen.reactive.pacman.agent.model.CaseStudy(caseStudy.upWeight, weight, caseStudy.downWeight, caseStudy.leftWeight);
            case 2:
                return new org.coinen.reactive.pacman.agent.model.CaseStudy(caseStudy.upWeight, caseStudy.rightWeight, weight, caseStudy.leftWeight);
            case 3:
                return new org.coinen.reactive.pacman.agent.model.CaseStudy(caseStudy.upWeight, caseStudy.rightWeight, caseStudy.downWeight, weight);
        }

        throw new IllegalArgumentException("Unsupported Direction");
    }

    public static float normalizedDistance(float f) {
        return f / MAX_STATE_DISTANCE;
    }

    /*
     * Returns index of most similar case to case s given, as long as it's close enough, depending on the threshold.
     * */
    public static int searchCase(GameState s) {
        int result = -1;
        comp = Q_learn.MAX_STATE_DISTANCE;
        currBest = Q_learn.MAX_STATE_DISTANCE;

        for (int i = 0; i < S.size(); i++) {
            comp = compareCases(s, S.get(i));
            if (comp < currBest) {
                currBest = comp;
                result = i;
            }
        }


        currBest = normalizedDistance(currBest);

        if (currBest < newCaseThreshold) {
            currSimilarity = currBest;
            similarities.add(currSimilarity);
            return result;
        } else {
            currSimilarity = 1;
            similarities.add(currSimilarity);
            return -1;
        }
    }

    /*
     * Multithreaded search of similar cases in casebase. Returns most similar case if it is close enough.
     * Otherwise it returns -1.
     * */
    public static int searchCaseWithThreads(GameState s) throws InterruptedException {

        int result;
        comp = Q_learn.MAX_STATE_DISTANCE;
        currBest = Q_learn.MAX_STATE_DISTANCE;
        int n_threads = 2;

        SearchThread R[] = new SearchThread[n_threads];


        for (int i = 0; i < n_threads; i++) {
            R[i] = new SearchThread("Thread-" + i, (int) i * S.size() / n_threads, (int) (i + 1) * (S.size() / n_threads), s);
            R[i].start();
        }

        for (int i = 0; i < n_threads; i++) {
            R[i].join();
        }

        if (currBest < Q_learn.newCaseThreshold) { // Retrieve case
            currSimilarity = (float) currBest;
            similarities.add(currSimilarity);
            result = searchResult;
        } else {                // No similar enough case found
            currSimilarity = 1;
            //System.out.println("NEW CASE! " + currBest);
            //System.out.println("----------------");
            //similarities.add(currSimilarity);
            result = -1;
        }

        //float currBest = Math.min(R1.currSimilarity, R2.currSimilarity);

        return result;
    }


    public static int searchForClean(GameState s) throws InterruptedException {

        int result;
        comp = Q_learn.MAX_STATE_DISTANCE;
        currBest = Q_learn.MAX_STATE_DISTANCE;
        int n_threads = 3;

        SearchThread R[] = new SearchThread[n_threads];
        R[0] = new SearchThread("Thread-1", 0, (int) S.size() / n_threads, s);
        R[1] = new SearchThread("Thread-2", (int) S.size() / n_threads, (int) 2 * (S.size() / n_threads), s);
        R[2] = new SearchThread("Thread-3", (int) 2 * (S.size() / n_threads), S.size(), s);
        R[0].start();
        R[1].start();
        R[2].start();


        for (int i = 0; i < n_threads; i++) {
            R[i].join();
        }


        result = searchResult;
        if (result == -1)
            System.out.println("-1 en SFC");

        return result;
    }


    /* Deletes cases when case base is full*/
    public static void cleanCaseBase() throws InterruptedException {

        int deletedCases = 0;


        while (deletedCases < CASES_TO_DELETE) {
            int rc = G.rnd.nextInt(S.size());    // Select a random case
            int msc = searchForClean(S.get(rc));    // Search for most similar case
            int e;

            if (msc == -1)
                System.out.println("-1 en SFC");

            if (maxQRowIndex(rc) == maxQRowIndex(msc)) {
                if (stateCounter.get(rc) > stateCounter.get(msc)) e = msc;
                else e = rc;
                S.remove(e);    // If they have the same best movement, remove most similar case
                Q.remove(e);
                currQ.remove(e);
                stateCounter.remove(e);
                deletedCases++;
            }
        }

        correctIndexes();


    }


    public static void correctIndexes() {
//        for (int i = 0; i < S.size(); i++) S.get(i).index = i;
    }


    /* This function implements similarity measure between two cases*/
    public static float compareCases(GameState s, GameState s2) {
        float result = 0;


        float distPills = 0;
        if (s.pillUp != s2.pillUp) distPills++;
        if (s.pillDown != s2.pillDown) distPills++;
        if (s.pillLeft != s2.pillLeft) distPills++;
        if (s.pillRight != s2.pillRight) distPills++;
        //distPills /= 4.0f;*/

        float distGhosts = 0;
        if (s.ghostUp != s2.ghostUp) distGhosts++;
        if (s.ghostDown != s2.ghostDown) distGhosts++;
        if (s.ghostLeft != s2.ghostLeft) distGhosts++;
        if (s.ghostRight != s2.ghostRight) distGhosts++;
        //distGhosts /= 4.0f;

        float distPowerPills = 0;
        if (s.powerPillUp != s2.powerPillUp) distPowerPills++;
        if (s.powerPillDown != s2.powerPillDown) distPowerPills++;
        if (s.powerPillLeft != s2.powerPillLeft) distPowerPills++;
        if (s.powerPillRight != s2.powerPillRight) distPowerPills++;
//		distPowerPills /= 4.0f;

        float distEdibleGhosts = 0;
        if (s.edibleGhostUp != s2.edibleGhostUp) distEdibleGhosts++;
        if (s.edibleGhostDown != s2.edibleGhostDown) distEdibleGhosts++;
        if (s.edibleGhostLeft != s2.edibleGhostLeft) distEdibleGhosts++;
        if (s.edibleGhostRight != s2.edibleGhostRight) distEdibleGhosts++;
//		

        float distIntersection = 0;
        if (s.intersectionUp != s2.intersectionUp) distIntersection++;
        if (s.intersectionDown != s2.intersectionDown) distIntersection++;
        if (s.intersectionLeft != s2.intersectionLeft) distIntersection++;
        if (s.intersectionRight != s2.intersectionRight) distIntersection++;


        result = w1 * distPills + w2 * distGhosts + w3 * distPowerPills + w4 * distEdibleGhosts + w5 * distIntersection;


        return result;
    }


    /*
     * Returns distance between two vectors x and y [eucl = sum(x_i-y_i) ]
     * */
    public static int euclidean(int[] x, int[] y) {
        int sum = 0;
        for (int i = 0; i < x.length; i++) {
            if (x[i] == -1 && y[i] == -1)
                sum += 0;                        // When there is no pill in that dir. in both states, distance is 0
            else if (x[i] == -1 || y[i] == -1)
                sum += MAX_MAP_DISTANCE;        // When only one of the states has a pill in that dir, distance is max. (set to 100 in discretization process, since x-y is always less than that)
            else if (x[i] >= y[i]) sum += x[i] - y[i];                        // Otherwise distance is (x-y)^2
            else sum += y[i] - x[i];
        }

        return sum;
    }


    /* Returns the sum of distance differences between ghosts in both states*/
	/*public static int compareGhosts(Game game, GameState s, GameState s2){
		int result = 0;
		
		for(int i = 0; i<4; i++){
			result += game.getPathDistance(s.ghostLoc[i], s2.ghostLoc[i]);
		}
		
		return result;
	}
	*/

    /*Returns number of pills that are active in both cases*/
	/*public static int comparePills(Game game, GameState s, GameState s2){
		int result = 0;
		int n1 = s.pillIndicesActive.length;
		int n2 = s2.pillIndicesActive.length;
		int n = Math.max(n1, n2);
		
		for(int i=0; i<n2; i++){
			if(Arrays.asList(s.pillIndicesActive).contains(s2.pillIndicesActive[i])) result++;
		}
		
		
		return result;
	}
*/
    /*Returns number of power pills that are active in both cases*/
	/*public static int comparePowerPills(Game game, GameState s, GameState s2){
		int result = 0;
		int n1 = s.powerPillIndicesActive.length;
		int n2 = s2.powerPillIndicesActive.length;
		int n = Math.max(n1, n2);
		
		for(int i=0; i<n2; i++){
			if(Arrays.asList(s.powerPillIndicesActive).contains(s2.powerPillIndicesActive[i])) result++;
		}
		
		
		return result;
	}
	*/
//    public static int[] ghostLocs(Game game) {
//        int[] ghostLocs = new int[4];
//
//        ghostLocs[0] = game.getCurGhostLoc(0);
//        ghostLocs[1] = game.getCurGhostLoc(1);
//        ghostLocs[2] = game.getCurGhostLoc(2);
//        ghostLocs[3] = game.getCurGhostLoc(3);
//
//        return ghostLocs;
//    }


    public static boolean closeGhost() {


        if ((currState.ghostUp == GameUtils.DISCRETE_VERY_CLOSE) || (currState.ghostUp == GameUtils.DISCRETE_CLOSE) ||
                (currState.ghostRight == GameUtils.DISCRETE_VERY_CLOSE) || (currState.ghostRight == GameUtils.DISCRETE_CLOSE) ||
                (currState.ghostDown == GameUtils.DISCRETE_VERY_CLOSE) || (currState.ghostDown == GameUtils.DISCRETE_CLOSE) ||
                (currState.ghostLeft == GameUtils.DISCRETE_VERY_CLOSE) || (currState.ghostLeft == GameUtils.DISCRETE_CLOSE)/* ||
				(currState.edibleGhostUp <= GameUtils.DISCRETE_VERY_CLOSE && currState.edibleGhostUp >= 0) || 
				(currState.edibleGhostRight <= GameUtils.DISCRETE_VERY_CLOSE && currState.edibleGhostRight >= 0) || 
				(currState.edibleGhostDown <= GameUtils.DISCRETE_VERY_CLOSE && currState.edibleGhostDown >= 0) ||
				(currState.edibleGhostLeft <= GameUtils.DISCRETE_VERY_CLOSE && currState.edibleGhostLeft >= 0)*/)
            return true;

        return false;
    }

    public static boolean closeGhost(GameState currState) {


        if ((currState.ghostUp == GameUtils.DISCRETE_VERY_CLOSE) || (currState.ghostUp == GameUtils.DISCRETE_CLOSE) ||
                (currState.ghostRight == GameUtils.DISCRETE_VERY_CLOSE) || (currState.ghostRight == GameUtils.DISCRETE_CLOSE) ||
                (currState.ghostDown == GameUtils.DISCRETE_VERY_CLOSE) || (currState.ghostDown == GameUtils.DISCRETE_CLOSE) ||
                (currState.ghostLeft == GameUtils.DISCRETE_VERY_CLOSE) || (currState.ghostLeft == GameUtils.DISCRETE_CLOSE)/* ||
				(currState.edibleGhostUp <= GameUtils.DISCRETE_VERY_CLOSE && currState.edibleGhostUp >= 0) ||
				(currState.edibleGhostRight <= GameUtils.DISCRETE_VERY_CLOSE && currState.edibleGhostRight >= 0) ||
				(currState.edibleGhostDown <= GameUtils.DISCRETE_VERY_CLOSE && currState.edibleGhostDown >= 0) ||
				(currState.edibleGhostLeft <= GameUtils.DISCRETE_VERY_CLOSE && currState.edibleGhostLeft >= 0)*/)
            return true;

        return false;
    }


    /* Adds average value between score provided and previous scores*/
    public static void addNewScore(int score) {

        int[] buf = new int[100];

        scores.add(score);


    }

    public static void calculateDrawableScores() {

        final int n = 20;
        int[] buf = new int[n];
        int[] buf2 = new int[n];
        int[] bufRnd = new int[n];
        int[] bufCP = new int[n];

        for (int i = 0; i < scores.size(); i++) {
            buf[i % n] = scores.get(i);
            buf2[i % n] = times.get(i);
            //bufRnd[i%n] = randomScores.get(i);
            //bufCP[i%n] = closestPillScores.get(i);
            if (i % n == n - 1) {
                drawableScores.add(intMean(buf));
                drawableTimes.add(intMean(buf2));
                // drawableRandomScores.add(intMean(bufRnd));
                //drawableClosestPillScores.add(intMean(bufCP));
            }

        }


    }


    /* Calculate simulation stats*/
    public static void stats() {

        //final int n = scores.size();
        final int n = scores.size();
        int[] t = new int[n];
        int[] s = new int[n];
        int[] l = new int[n];
        int[] bufRnd = new int[n];
        int[] bufCP = new int[n];

        maxScore = Collections.max(scores);
        maxTime = Collections.max(times);
        maxLevel = Collections.max(levelsCompleted);

        for (int i = 0; i < n; i++) {
            s[i] = scores.get(scores.size() - 1 - i);
            t[i] = times.get(scores.size() - 1 - i);
        }
		/*
		for(int i = 0; i < n; i++){
			s[i] = scores.get(i);
			t[i] = times.get(i);
			
		}*/

        for (int i = 0; i < levelsCompleted.size(); i++) {
            l[i] = levelsCompleted.get(i);
        }

        avgScore = intMean(s);
        avgTime = intMean(t);
        avgLevels = mean(l);

        stdScore = getStdDev(s);
        stdTime = getStdDev(t);


    }

    /*Returns integer average value of elements in vector m*/
    public static int intMean(int[] m) {
        double sum = 0;
        for (int i = 0; i < m.length; i++) {
            sum += m[i];
        }
        return (int) sum / m.length;
    }


    public static float mean(int[] m) {
        float sum = 0;
        for (int i = 0; i < m.length; i++) {
            sum += m[i];
        }
        return sum / m.length;
    }

    public static float getVariance(int[] m) {
        int mean = intMean(m);
        float temp = 0;
        for (int i = 0; i < m.length; i++)
            temp += (m[i] - mean) * (m[i] - mean);
        return temp / m.length;
    }

    public static float getStdDev(int[] m) {
        return (float) Math.sqrt(getVariance(m));
    }


    /*Returns integer average value of elements in vector m*/
    public static float averageSimilarity(List<Float> s) {
        float sum = 0;
        for (int i = 0; i < s.size(); i++) {
            sum += s.get(i);
        }
        return sum / s.size();
    }

    /*Calculates distances from each node to all of them in current level*/
    public static void calculateDistances(Game game) {
        PrintWriter writer;
        try {
            writer = new PrintWriter("distances.txt", "UTF-8");


            for (int i = 0; i < game.getNumberOfNodes(); i++) {
                distances.add(new ArrayList<>());

                for (int j = 0; j < game.getNumberOfNodes(); j++) {
                    distances.get(i).add(game.getPathDistance(i, j));
                    writer.print(distances.get(i).get(j) + " ");
                }
                writer.print("\n");
            }

            writer.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /*Calculates distances from each node to all of them in given direction of current level*/
    public static void calculateDistancesInDirection(Game game, int direction) {
        PrintWriter writer;
        try {
            writer = new PrintWriter("distances" + direction + ".txt", "UTF-8");
            System.out.println("Calculating distances in direction:  " + direction);
            for (int i = 0; i < game.getNumberOfNodes(); i++) {
                for (int j = 0; j < game.getNumberOfNodes(); j++) {
                    writer.print(DistanceInDirection(game, direction, i, j) + " ");
                }

                writer.print("\n");
            }

            writer.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static int DistanceInDirection(Game game, int direction, int source, int node) {

        Set<Integer> sSet = new HashSet<>();
        List<Integer> s = new ArrayList<>();
        s.add(source);
        int root = game.getNeighbour(source, direction);
        int distance = 0;


        if (source == node) return 0;
        if (root != -1) {
            s.add(root);
            Queue<Integer> q = new ArrayDeque<>();
            q.add(root);
            int current;

            while (!q.isEmpty()) {
                current = q.remove();
                if (current == node) return distance + 1;
                distance++;

                //int directions[] = game.getPossiblePacManDirs(false);
                for (int i = 0; i < 4; i++) {
                    int n = game.getNeighbour(current, i);
                    if (!sSet.contains(n) && n != -1) {
                        s.add(n);
                        sSet.add(n);
                        q.add(n);
                    }
                }
            }
        }
        return -1;

    }

    public static int getDistance(int direction, int from, int to) {
        int dist;

        ArrayList<ArrayList<Integer>> du = currDistancesUp;
        ArrayList<ArrayList<Integer>> dr = currDistancesRight;
        ArrayList<ArrayList<Integer>> dd = currDistancesDown;
        ArrayList<ArrayList<Integer>> dl = currDistancesLeft;

        if (direction == 0) dist = du.get(from).get(to);
        else if (direction == 1) dist = dr.get(from).get(to);
        else if (direction == 2) dist = dd.get(from).get(to);
        else dist = dl.get(from).get(to);

        return dist;
    }

    public static void changeDistances(int level) {
        switch (level) {
            case 0: {
                Q_learn.currDistancesUp = Q_learn.distancesUp;
                Q_learn.currDistancesRight = Q_learn.distancesRight;
                Q_learn.currDistancesDown = Q_learn.distancesDown;
                Q_learn.currDistancesLeft = Q_learn.distancesLeft;
                break;
            }
            case 1: {
                Q_learn.currDistancesUp = Q_learn.distances2Up;
                Q_learn.currDistancesRight = Q_learn.distances2Right;
                Q_learn.currDistancesDown = Q_learn.distances2Down;
                Q_learn.currDistancesLeft = Q_learn.distances2Left;
                break;
            }
            case 2: {
                Q_learn.currDistancesUp = Q_learn.distances3Up;
                Q_learn.currDistancesRight = Q_learn.distances3Right;
                Q_learn.currDistancesDown = Q_learn.distances3Down;
                Q_learn.currDistancesLeft = Q_learn.distances3Left;
                break;
            }
            case 3: {
                Q_learn.currDistancesUp = Q_learn.distances4Up;
                Q_learn.currDistancesRight = Q_learn.distances4Right;
                Q_learn.currDistancesDown = Q_learn.distances4Down;
                Q_learn.currDistancesLeft = Q_learn.distances4Left;
                break;
            }
            default: {
                Q_learn.currDistancesUp = Q_learn.distancesUp;
                Q_learn.currDistancesRight = Q_learn.distancesRight;
                Q_learn.currDistancesDown = Q_learn.distancesDown;
                Q_learn.currDistancesLeft = Q_learn.distancesLeft;
                break;
            }

        }


    }

    // Stores all information needed for drawing plots, and checks if case base is full
    public static void endEpisode() {


        numberOfCases.add(S.size());

        addedCases.add(currAdded);
        currAdded = 0;

        levelsCompleted.add(currLevels);
        currLevels = 0;

        retrievedCases.add(currRetrieved);
        currRetrieved = 0;

        averageSimilarities.add(averageSimilarity(similarities));


        if (S.size() > MAX_CASES) {
            try {
                cleanCaseBase();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }


    }


}