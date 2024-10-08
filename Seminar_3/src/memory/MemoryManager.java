package memory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;

public class MemoryManager {

	private Iterator<Integer> it;
	private LinkedHashSet<Integer> lruSet;
	private Queue<Integer> pageQueue;
	private int myNumberOfPages;
	private int myPageSize; // In bytes
	private int myNumberOfFrames;
	private int[] myPageTable; // -1 if page is not in physical memory
	private byte[] myRAM; // physical memory RAM
	private RandomAccessFile myPageFile;
	private int myNextFreeFramePosition = 0;
	private int myNumberOfpageFaults = 0;
	private int myPageReplacementAlgorithm = 0;

	public MemoryManager(int numberOfPages, int pageSize, int numberOfFrames, String pageFile,
			int pageReplacementAlgorithm) {

		myNumberOfPages = numberOfPages;
		myPageSize = pageSize;
		myNumberOfFrames = numberOfFrames;
		myPageReplacementAlgorithm = pageReplacementAlgorithm;
		pageQueue = new LinkedList<>();
		lruSet = new LinkedHashSet<>();
		Iterator<Integer> it = lruSet.iterator();

		initPageTable();
		myRAM = new byte[myNumberOfFrames * myPageSize];

		try {

			myPageFile = new RandomAccessFile(pageFile, "r");

		} catch (FileNotFoundException ex) {
			System.out.println("Can't open page file: " + ex.getMessage());
		}
	}

	private void initPageTable() {
		myPageTable = new int[myNumberOfPages];
		for (int n = 0; n < myNumberOfPages; n++) {
			myPageTable[n] = -1;
		}
	}

	public byte readFromMemory(int logicalAddress) {
		int pageNumber = getPageNumber(logicalAddress);
		int offset = getPageOffset(logicalAddress);

		if (myPageTable[pageNumber] == -1) {
			pageFault(pageNumber);
		}

		int frame = myPageTable[pageNumber];
		int physicalAddress = frame * myPageSize + offset;
		byte data = myRAM[physicalAddress];

		System.out.print("Virtual address: " + logicalAddress);
		System.out.print(" Physical address: " + physicalAddress);
		System.out.println(" Value: " + data);
		return data;
	}
	public static int getPageNumber(int logicalAddress) {
		// shifts the address by 8 bits to the right
		return logicalAddress >> 8;
	}

	public static int getPageOffset(int logicalAddress) {
		// last 8 bits is the offset
		return logicalAddress & 0xFF;
	}

	private void pageFault(int pageNumber) {
		if (myPageReplacementAlgorithm == Seminar3.NO_PAGE_REPLACEMENT)
			handlePageFault(pageNumber);

		if (myPageReplacementAlgorithm == Seminar3.FIFO_PAGE_REPLACEMENT)
			handlePageFaultFIFO(pageNumber);

		if (myPageReplacementAlgorithm == Seminar3.LRU_PAGE_REPLACEMENT)
			handlePageFaultLRU(pageNumber);

		readFromPageFileToMemory(pageNumber);
	}

	private void readFromPageFileToMemory(int pageNumber) {
		try {
			int frame = myPageTable[pageNumber];
			myPageFile.seek(pageNumber * myPageSize);
			for (int b = 0; b < myPageSize; b++)
				myRAM[frame * myPageSize + b] = myPageFile.readByte();
		} catch (IOException ex) {

		}
	}

	public int getNumberOfPageFaults() {
		return myNumberOfpageFaults;
	}

	private void handlePageFault(int pageNumber) {
		// checking if there is enough size in myRAM
		if (myNextFreeFramePosition * myPageSize + myPageSize > myRAM.length) {
			throw new OutOfMemoryError("No enough space available in RAM");
		}

		// checking if the page number is valid
		if (pageNumber < 0 || pageNumber >= myPageTable.length) {
			throw new IllegalArgumentException("Invalid page number: " + pageNumber);
		}

		// maps the page number to the position in the memory
		myPageTable[pageNumber] = myNextFreeFramePosition;
		myNextFreeFramePosition += 1;
		readFromPageFileToMemory(pageNumber);
		myNumberOfpageFaults ++;
	}

	private void handlePageFaultFIFO(int pageNumber) {

		// if all frames are used up
		if (pageQueue.size() >= myRAM.length / myPageSize) {

			// take out the oldest page
			int pageToReplace = pageQueue.poll();

			// take out its frame
			int frameToReplace = myPageTable[pageToReplace];
			myPageTable[pageToReplace] = -1;

			// assign the new page number to that frame
			myPageTable[pageNumber] = frameToReplace;
		}

		// if there is space
		else {
			myPageTable[pageNumber] = myNextFreeFramePosition;
			myNextFreeFramePosition++;
		}

		pageQueue.add(pageNumber);
		readFromPageFileToMemory(pageNumber);
		myNumberOfpageFaults ++;
	}
	private void handlePageFaultLRU(int pageNumber) {
		// contains the page number
		if (lruSet.contains(pageNumber)) {
			lruSet.remove(pageNumber);
		}
		// myRam is full
		else if (lruSet.size() >= myRAM.length / myPageSize) {
			// remove the least recently used page
			int lruPageNumber = lruSet.removeFirst();

			// remove its corresponding frame
			int frameToReplace = myPageTable[lruPageNumber];
			myPageTable[lruPageNumber] = -1;

			// replace the new page number with that frame
			myPageTable[pageNumber] = frameToReplace;
			myNumberOfpageFaults++;
		}
		// myRam has free space
		else {
			myPageTable[pageNumber] = myNextFreeFramePosition;
			myNextFreeFramePosition ++;
			myNumberOfpageFaults++;
		}


		lruSet.add(pageNumber);
		readFromPageFileToMemory(pageNumber);
	}
}
