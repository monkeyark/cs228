package edu.iastate.cs228.hw3;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;


/**
 * This class represents a board configuration in the 8-puzzle.  Only the initial configuration is 
 * generated by a constructor, while intermediate configurations will be generated via calling
 * the method successorState().  State objects will form two circular doubly-linked lists OPEN and 
 * CLOSED, which will be used by the A* algorithm to search for a path from a given initial board
 * configuration to the final board configuration below: 
 * 
 *  1 2 3 
 *  8   4
 *  7 6 5
 *
 * The final configuration (i.e., the goal state) above is not explicitly represented as an object 
 * of the State class. 
 */

public class State implements Cloneable, Comparable<State>
{
	public int[][] board; 		// configuration of tiles 
	
	public State previous;    	// previous node on the OPEN/CLOSED list
	public State next; 			// next node on the OPEN/CLOSED list
	public State predecessor; 	// predecessor node on the path from the initial state 
	
	public Move move;           // the move that generated this state from its predecessor
	public int numMoves; 	    // number of moves from the initial state to this state
	
	public static Heuristic heu; // heuristic used. shared by all the states. 
	
	private int numMismatchedTiles = -1;  // number of mismatched tiles between this state 
	                                      // and the goal state; negative if not computed yet.
	private int ManhattanDistance = -1;   // Manhattan distance between this state and the 
	                                      // goal state; negative if not computed yet.
	
	private final int[][] GOAL = {{1,2,3}, {8,0,4}, {7,6,5}};
	
	/**
	 * Constructor (for the initial state).
	 * 
	 * It takes a 2-dimensional array representing an initial board configuration. The empty 
	 * square is represented by the number 0.  
	 * 
	 *     a) Initialize all three links previous, next, and predecessor to null.  
	 *     b) Set move to null and numMoves to zero.
	 * 
	 * @param board
	 * @throws IllegalArgumentException		if board is not a 3X3 array or its nine entries are 
	 * 										not respectively the digits 0, 1, ..., 8. 
	 */
    public State(int[][] board) throws IllegalArgumentException 
    {
    	if (board.length != 3 || board[0].length != 3 || board[1].length != 3 || board[2].length != 3) throw new IllegalArgumentException(); // check board dimensions
    	
    	this.board = new int[3][3];
    	HashMap<Integer, Integer> numberAndCount = new HashMap<Integer, Integer>();
    	
    	for (int i=0; i<board.length; i++)
    	{
    		for (int j=0; j<board[0].length; j++)
    		{
    			if(board[i][j]<0 || 8<board[i][j]) throw new IllegalArgumentException(); // check the range of element
    			
    			Integer count = numberAndCount.get(board[i][j]);
    			if (count == null)
    			{
    				numberAndCount.put(board[i][j], 1);
    				this.board[i][j] = board[i][j];
    			}
    			else // found duplicate
    			{
    				this.board = null; // re-initial the board
    				throw new IllegalArgumentException();
    			}
    		}
    	}
    	previous = null;
    	next = null;
    	predecessor = null;
    	move = null;
    	numMoves = 0;
	}
    
    /**
     * Constructor (for the initial state) 
     * 
     * It takes a state from an input file that has three rows, each containing three digits 
     * separated by exactly one blank.  Every row starts with a digit. The nine digits are 
     * from 0 to 8 with no duplicates.  
     * 
     * Do the same initializations as for the first constructor. 
     * 
     * @param inputFileName
     * @throws FileNotFoundException
     * @throws IllegalArgumentException  if the file content does not meet the above requirements. 
     */
    public State (String inputFileName) throws FileNotFoundException, IllegalArgumentException
    {
    	if (inputFileName == null) throw new FileNotFoundException();
    	
		File file = new File(inputFileName);
		Scanner scanner = new Scanner(file);
		int row = 0;
		HashMap<Integer, Integer> numberAndCount = new HashMap<Integer, Integer>();
		
		while (scanner.hasNextLine() && scanner.hasNext())
		{
			String nextLine = scanner.nextLine();
			String[] line = nextLine.split("\\s+");
			if (line.length != 3) // check board column
			{
				scanner.close();
				board = null; // re-initial the board
				throw new IllegalArgumentException();
			}
			
			for (int col=0; col<line.length; col++)
			{
				Integer count = numberAndCount.get(board[row][col]);
				if (count == null)
    			{
					numberAndCount.put(board[row][col], 1);
					board[row][col] = Integer.parseInt(line[col]);
    			}
				else
    			{
    				scanner.close();
    				this.board = null; // re-initial the board
    				throw new IllegalArgumentException();
    			}
			}
			row++;
			if (row >= 3) // check board column
			{
				scanner.close();
				board = null; // re-initial the board
				throw new IllegalArgumentException();	
			}
		}
		scanner.close();
		
    	previous = null;
    	next = null;
    	predecessor = null;
    	move = null;
    	numMoves = 0;
	}
    
    /**
     * Generate the successor state resulting from a given move.  Throw an exception if the move 
     * cannot be executed.  Besides setting the array board[][] properly, you also need to do the 
     * following:
     * 
     *     a) set the predecessor of the successor state to this state;
     *     b) set the private instance variable move of the successor state to the parameter m; 
     *     c) Set the links next and previous to null;  
     *     d) Update numMoves. 
     * 
     * @param m  one of the moves LEFT, RIGHT, UP, and DOWN
     * @return
     * @throws IllegalArgumentException if RIGHT when the empty square is in the left column, or 
     *                                  if LEFT when the empty square is in the right column, or 
     *                                  if UP when the empty square is in the bottom row, or 
     *                                  if DOWN when the empty square is in the top row. 
     */                                  
    public State successorState(Move m) throws IllegalArgumentException 
    {
    	int row = 0;
    	int col = 0;
    	//find location of 0
    	for (int i=0; i<board.length; i++)
    	{
    		for (int j=0; j<board.length; j++)
    		{
    			if (board[i][j] == 0)
    			{
					row = i;
					col = j;
    			}
    		}
    	}
    	//check Exception
    	if (m == Move.LEFT && col == 2
    		||m == Move.RIGHT && col == 0
    		||m == Move.UP && row == 2
    		||m == Move.DOWN && row == 0)
		{
			throw new IllegalArgumentException();
		}
    	//
    	State successor = (State) this.clone();
    		
		if (m == Move.LEFT)
		{
			successor.board[row][col] = successor.board[row][col+1];
			successor.board[row][col+1] = 0;
		}
		else if (m == Move.RIGHT)
		{
			successor.board[row][col] = successor.board[row][col-1];
			successor.board[row][col-1] = 0;
		}
		else if (m == Move.UP)
		{
			successor.board[row][col] = successor.board[row+1][col];
			successor.board[row+1][col] = 0;
		}
		else if (m == Move.DOWN)
		{
			successor.board[row][col] = successor.board[row-1][col];
			successor.board[row-1][col] = 0;
		}
    	
    	successor.predecessor = this;
    	successor.move = m;
    	successor.numMoves = numMoves + 1;
    	
    	return successor;
    }
        
    /**
     * Determines if the board configuration in this state can be rearranged into the goal configuration. 
     * According to the appendix in the project description, we check if this state has an odd number of 
     * inversions.
     * 
     * @return true if the puzzle starting in this state can be rearranged into the goal state.
     */
    public boolean solvable()
    {
    	ArrayList<Integer> list = new ArrayList<Integer>();
    	for (int i=0; i<board.length; i++)
    	{
    		for (int j=0; j<board.length; j++)
    		{
    			if (board[i][j] != 0)
    			{
    				list.add(board[i][j]);
    			}
    		}
    	}
    	
    	int inversion = 0;
    	for (int i=0; i<list.size(); i++)
    	{
    		for (int j=i+1; j<list.size(); j++)
    		{
        		if (list.get(i) > list.get(j))
        		{
        			inversion++;
        		}
    		}
    	}
    	return inversion%2 == 1; 
    }
    
    /**
     * Check if this state is the goal state, namely, if the array board[][] stores the following contents: 
     * 
     * 		1 2 3 
     * 		8 0 4 
     * 		7 6 5 
     * 
     * @return
     */
    public boolean isGoalState()
    {
    	return Arrays.deepEquals(board, GOAL); 
    }
    
    /**
     * Write the board configuration according to the following format:
     * 
     *     a) Output row by row in three lines with no indentations.  
     *     b) Two adjacent tiles in each row have exactly one blank in between. 
     *     c) The empty square is represented by a blank.  
     *     
     * For example, 
     * 
     * 2   3
     * 1 8 4
     * 7 6 5  
     * 
     */
    @Override 
    public String toString()
    {
    	String stateBoard = "";
		for (int i=0; i<board.length; i++)
		{
			for (int j=0; j<board.length; j++)
			{
				if (board[i][j] == 0)
				{
					stateBoard += " " + " ";
				}
				else
				{
					stateBoard += board[i][j] + " ";
				}
			}
			//stateBoard += System.lineSeparator();
			stateBoard += "\n";
		}
    	return stateBoard; 
    }
    
    /**
     * Create a clone of this State object by copying over the board[][]. Set the links previous,
     * next, and predecessor to null. 
     * 
     * The method is called by SuccessorState(); 
     * @throws CloneNotSupportedException 
     */
    @Override
    public Object clone()
    {
    	int[][] copyBoard = new int[board.length][board[0].length];
		for (int i=0; i<board.length; i++)
		{
			for (int j=0; j<board.length; j++)
			{
				copyBoard[i][j] = board[i][j];
			}
		}
	    return new State(copyBoard);
    }
    
    /**
     * Compare this state with the argument state.  Two states are equal if their arrays board[][] 
     * have the same content.
     */
    @Override
    public boolean equals(Object o)
    {
    	//StateComparator cc = new StateComparator();
    	if (o == null || o.getClass() != getClass())
    	{
    		return false;
    	}
    	
    	State other = (State) o;
    	
    	return other.board == board || other.board != null && Arrays.deepEquals(other.board, board);//TODO
//    	return (other.move == move)
//				&& (other.board == board || other.board != null && Arrays.equals(other.board, board))
//    			&& (other.numMoves == numMoves)
//    			&& (other.heu == heu)
//    			&& (other.numMismatchedTiles == numMismatchedTiles)
//    			&& (other.ManhattanDistance == ManhattanDistance)
//    			&& (other.previous == previous || other.previous != null /* && other.previous.equals(previous)*/)
//    			&& (other.next == next || other.next != null /* && other.next.equals(next)*/)
//    			&& (other.predecessor == predecessor || other.predecessor != null /* && other.predecessor.equals(predecessor)*/);
    }
    
    
    /**
     * Evaluate the cost of this state as the sum of the number of moves from the initial state and 
     * the estimated number of moves to the goal state using the heuristic stored in the instance 
     * variable heu. 
     * 
     * If heu == TileMismatch, add up numMoves and the return values from computeNumMismatchedTiles().
     * If heu == MahattanDist, add up numMoves and the return values of computeMahattanDistance(). 
     * 
     * @param h
     * @return estimated number of moves from the initial state to the goal state via this state.
     * @throws IllegalArgumentException if heuristic is neither 0 nor 1. 
     */
    public int cost() throws IllegalArgumentException
    {
    	int cost = 0;
    	if (heu == Heuristic.TileMismatch)
    	{
    		cost = numMoves + computeNumMismatchedTiles();
    	}
    	else if (heu == Heuristic.ManhattanDist)
    	{
    		cost =  numMoves + computeManhattanDistance();
    	}
    	else
    	{
    		throw new IllegalArgumentException();
    	}
    	return cost;
    }
    
    /**
     * Compare two states by the cost. Let c1 and c2 be the costs of this state and the argument state s.
     * 
     * @return -1 if c1 < c2 
     *          0 if c1 = c2 
     *          1 if c1 > c2 
     *          
     * Call the method cost(). This comparison will be used in maintaining the OPEN list by the A* algorithm.
     */
    @Override
    public int compareTo(State s)
    {
    	if (this.cost() < s.cost())
    	{
    		return -1;
    	}
    	else if (this.cost() == s.cost())
    	{
    		return 0;
    	}
    	else
    	{
    		return 1;
    	}
    }
    
    /**
     * Return the value of numMismatchedTiles if it is non-negative, and compute the value otherwise. 
     * 
     * @return number of mismatched tiles between this state and the goal state. 
     */
	private int computeNumMismatchedTiles()
	{
		if (numMismatchedTiles < 0)
		{
	    	numMismatchedTiles = 0;
	    	
	    	for (int i=0; i<board.length; i++)
			{
				for (int j=0; j<board.length; j++)
				{
					if (board[i][j] != 0 && board[i][j] != GOAL[i][j])
					{
						numMismatchedTiles++;
					}
				}
			}
		}
		return numMismatchedTiles; 
	}
	
	/**
	 * Return the value of ManhattanDistance if it is non-negative, and compute the value otherwise.
	 * @return Manhattan distance between this state and the goal state. 
	 */
	private int computeManhattanDistance()
	{
		/*   i j
		 * 1 0 0
		 * 2 0 1
		 * 3 0 2
		 * 8 1 0
		 * 0 1 1
		 * 4 1 2
		 * 7 2 0
		 * 6 2 1
		 * 5 2 2
		 */
		if (ManhattanDistance < 0)
		{
			ManhattanDistance = 0;
			for (int i=0; i<board.length; i++)
			{
				for (int j=0; j<board.length; j++)
				{
					if (board[i][j] == 1) ManhattanDistance += Math.abs(i-0) + Math.abs(j-0);
					else if (board[i][j] == 2) ManhattanDistance += Math.abs(i-0) + Math.abs(j-1);
					else if (board[i][j] == 3) ManhattanDistance += Math.abs(i-0) + Math.abs(j-2);
					else if (board[i][j] == 4) ManhattanDistance += Math.abs(i-1) + Math.abs(j-2);
					else if (board[i][j] == 5) ManhattanDistance += Math.abs(i-2) + Math.abs(j-2);
					else if (board[i][j] == 6) ManhattanDistance += Math.abs(i-2) + Math.abs(j-1);
					else if (board[i][j] == 7) ManhattanDistance += Math.abs(i-2) + Math.abs(j-0);
					else if (board[i][j] == 8) ManhattanDistance += Math.abs(i-1) + Math.abs(j-0);
				}
			}
		}
		return ManhattanDistance; 
	}
}
