package Model.DataInsight;

/* This file is copyright (c) 2008-2013 Philippe Fournier-Viger
 * 
 * This file is part of the SPMF DATA MINING SOFTWARE
 * (http://www.philippe-fournier-viger.com/spmf).
 * 
 * SPMF is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * SPMF is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with
 * SPMF. If not, see <http://www.gnu.org/licenses/>.
 */
import Model.Item;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import DataInsight.MemoryLogger;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * This is an implementation of the FPGROWTH algorithm (Han et al., 2004). FPGrowth is described here:
 * <br/><br/>
 *
 * Han, J., Pei, J., & Yin, Y. (2000, May). Mining frequent patterns without candidate generation. In ACM SIGMOD Record (Vol. 29, No. 2, pp. 1-12). ACM
 * <br/><br/>
 *
 * This is an optimized version that saves the result to a file or keep it into memory if no output path is provided by the user to the runAlgorithm method().
 *
 * @see FPTree
 * @see Itemset
 * @see Itemsets
 * @author Philippe Fournier-Viger
 */
public class AlgoFPGrowth {

    // for statistics
    private long startTimestamp; // start time of the latest execution
    private long endTime; // end time of the latest execution
    private int transactionCount = 0; // transaction count in the database
    private int itemsetCount; // number of freq. itemsets found

    // parameter
    public int relativeMinsupp;// the relative minimum support

    // The  patterns that are found 
    // (if the user want to keep them into memory)
    protected Itemsets patterns = null;

    private MemoryLogger memoryLogger = null;

    /**
     * Constructor
     */
    public AlgoFPGrowth() {

    }

    /**
     * Method to run the FPGRowth algorithm.
     *
     * @param input the path to an input file containing a transaction database.
     * @param output the output file path for saving the result (if null, the result will be returned by the method instead of being saved).
     * @param minsupp the minimum support threshold.
     * @return the result if no output file path is provided.
     * @throws IOException exception if error reading or writing files
     */
    public Itemsets runAlgorithm(Map map, double minsupp) throws FileNotFoundException, IOException {
        // record start time
        startTimestamp = System.currentTimeMillis();
        // number of itemsets found
        itemsetCount = 0;
        patterns = new Itemsets("FREQUENT ITEMSETS");
        //initialize tool to record memory usage
        memoryLogger = new MemoryLogger();
        memoryLogger.checkMemory();

        // (1) PREPROCESSING: Initial database scan to determine the frequency of each item
        // The frequency is stored in a map:
        //    key: item   value: support
        final Map<Integer, Integer> mapSupport = new HashMap<Integer, Integer>();

        scanDatabaseToDetermineFrequencyOfSingleItems(map, mapSupport);

        // convert the minimum support as percentage to a
        // relative minimum support
        this.relativeMinsupp = (int) Math.ceil(minsupp * transactionCount);

        // (2) Scan the database again to build the initial FP-Tree
        // Before inserting a transaction in the FPTree, we sort the items
        // by descending order of support.  We ignore items that
        // do not have the minimum support.
        FPTree tree = new FPTree();

        Iterator it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();

            List<Item> trans = (List<Item>) pairs.getValue();
            List<Integer> transaction = new ArrayList();

            // for each item in the transaction
            for (int i = 0; i < trans.size(); i++) {
                Integer item = trans.get(i).createdInt;
                // only add items that have the minimum support

                if (mapSupport.get(item) >= relativeMinsupp) {
                    transaction.add(item);

                }
            }
            // sort item in the transaction by descending order of support
            Collections.sort(transaction, new Comparator<Integer>() {
                public int compare(Integer item1, Integer item2) {
                    // compare the frequency
                    int compare = mapSupport.get(item2) - mapSupport.get(item1);
                    // if the same frequency, we check the lexical ordering!
                    if (compare == 0) {
                        return (item1 - item2);
                    }
                    // otherwise, just use the frequency
                    return compare;
                }
            });
            // add the sorted transaction to the fptree.
            tree.addTransaction(transaction);
        }

        // We create the header table for the tree
        tree.createHeaderList(mapSupport);

        // (5) We start to mine the FP-Tree by calling the recursive method.
        // Initially, the prefix alpha is empty.
        int[] prefixAlpha = new int[0];
        // if at least an item is frequent
        if (tree.headerList.size() > 0) {
            fpgrowth(tree, prefixAlpha, transactionCount, mapSupport);
        }

        // record the execution end time
        endTime = System.currentTimeMillis();

        // check the memory usage
        memoryLogger.checkMemory();

        // return the result (if saved to memory)
        return patterns;

    }

    /**
     * This method scans the input database to calculate the support of single items
     *
     * @param input the path of the input file
     * @param mapSupport a map for storing the support of each item (key: item, value: support)
     * @throws IOException exception if error while writing the file
     */
    private void scanDatabaseToDetermineFrequencyOfSingleItems(Map map,
            final Map<Integer, Integer> mapSupport)
            throws FileNotFoundException, IOException {
        int numberofT = 0;
        Iterator it = map.entrySet().iterator();
        while (it.hasNext()) {
            numberofT++;
            Map.Entry pairs = (Map.Entry) it.next();

            List<Item> transaction = (List<Item>) pairs.getValue();
            // for each item in this line (transaction)

            for (int i = 0; i < transaction.size(); i++) {

                // transform this item from a string to an integer
                Item item = transaction.get(i);

                // increase the support count of the item
                Integer count = mapSupport.get(item.createdInt);
                if (count == null) {
                    mapSupport.put(item.createdInt, 1);
                } else {
                    mapSupport.put(item.createdInt, ++count);
                }
            }
            // increase the transaction count
            transactionCount++;
        }
        System.out.println("antall trans funnet " + numberofT);

    }

    /**
     * This method mines pattern from a Prefix-Tree recursively
     *
     * @param tree The Prefix Tree
     * @param prefix The current prefix "alpha"
     * @param mapSupport The frequency of each item in the prefix tree.
     * @throws IOException exception if error writing the output file
     */
    private void fpgrowth(FPTree tree, int[] prefixAlpha, int prefixSupport, Map<Integer, Integer> mapSupport) throws IOException {
        // We need to check if there is a single path in the prefix tree or not.
        if (tree.hasMoreThanOnePath == false) {
            // That means that there is a single path, so we 
            // add all combinations of this path, concatenated with the prefix "alpha", to the set of patterns found.
            if (tree.root.childs.size() == 0) {
                System.out.println("Test");
            }
            addAllCombinationsForPathAndPrefix(tree.root.childs.get(0), prefixAlpha); // CORRECT?

        } else { // There is more than one path
            fpgrowthMoreThanOnePath(tree, prefixAlpha, prefixSupport, mapSupport);
        }
    }

    /**
     * Mine an FP-Tree having more than one path.
     *
     * @param tree the FP-tree
     * @param prefix the current prefix, named "alpha"
     * @param mapSupport the frequency of items in the FP-Tree
     * @throws IOException exception if error writing the output file
     */
    private void fpgrowthMoreThanOnePath(FPTree tree, int[] prefixAlpha, int prefixSupport, Map<Integer, Integer> mapSupport) throws IOException {
        // For each frequent item in the header table list of the tree in reverse order.
        for (int i = tree.headerList.size() - 1; i >= 0; i--) {
            // get the item
            Integer item = tree.headerList.get(i);

            // get the support of the item
            int support = mapSupport.get(item);
            // if the item is not frequent, we skip it
            if (support < relativeMinsupp) {
                continue;
            }
            // Create Beta by concatening Alpha with the current item
            // and add it to the list of frequent patterns
            int[] beta = new int[prefixAlpha.length + 1];
            System.arraycopy(prefixAlpha, 0, beta, 0, prefixAlpha.length);
            beta[prefixAlpha.length] = item;

            // calculate the support of beta
            int betaSupport = (prefixSupport < support) ? prefixSupport : support;
            // save beta to the output file
            saveItemset(beta, betaSupport);

            // === Construct beta's conditional pattern base ===
            // It is a subdatabase which consists of the set of prefix paths
            // in the FP-tree co-occuring with the suffix pattern.
            List<List<FPNode>> prefixPaths = new ArrayList<List<FPNode>>();
            FPNode path = tree.mapItemNodes.get(item);
            while (path != null) {
                // if the path is not just the root node
                if (path.parent.itemID != -1) {
                    // create the prefixpath
                    List<FPNode> prefixPath = new ArrayList<FPNode>();
                    // add this node.
                    prefixPath.add(path);   // NOTE: we add it just to keep its support,
                    // actually it should not be part of the prefixPath

                    //Recursively add all the parents of this node.
                    FPNode parent = path.parent;
                    while (parent.itemID != -1) {
                        prefixPath.add(parent);
                        parent = parent.parent;
                    }
                    // add the path to the list of prefixpaths
                    prefixPaths.add(prefixPath);
                }
                // We will look for the next prefixpath
                path = path.nodeLink;
            }

            // (A) Calculate the frequency of each item in the prefixpath
            // The frequency is stored in a map such that:
            // key:  item   value: support
            Map<Integer, Integer> mapSupportBeta = new HashMap<Integer, Integer>();
            // for each prefixpath
            for (List<FPNode> prefixPath : prefixPaths) {
                // the support of the prefixpath is the support of its first node.
                int pathCount = prefixPath.get(0).counter;
                // for each node in the prefixpath,
                // except the first one, we count the frequency
                for (int j = 1; j < prefixPath.size(); j++) {
                    FPNode node = prefixPath.get(j);
                    // if the first time we see that node id
                    if (mapSupportBeta.get(node.itemID) == null) {
                        // just add the path count
                        mapSupportBeta.put(node.itemID, pathCount);
                    } else {
                        // otherwise, make the sum with the value already stored
                        mapSupportBeta.put(node.itemID, mapSupportBeta.get(node.itemID) + pathCount);
                    }
                }
            }

            // (B) Construct beta's conditional FP-Tree
            // Create the tree.
            FPTree treeBeta = new FPTree();
            // Add each prefixpath in the FP-tree.
            for (List<FPNode> prefixPath : prefixPaths) {
                treeBeta.addPrefixPath(prefixPath, mapSupportBeta, relativeMinsupp);
            }
            // Create the header list.
            treeBeta.createHeaderList(mapSupportBeta);

            // Mine recursively the Beta tree if the root as child(s)
            if (treeBeta.root.childs.size() > 0) {
                // recursive call
                fpgrowth(treeBeta, beta, betaSupport, mapSupportBeta);
            }
        }

    }

    /**
     * This method is for adding recursively all combinations of nodes in a path, concatenated with a given prefix, to the set of patterns found.
     *
     * @param prefix the prefix
     * @param minsupportForNode the support of this path.
     * @throws IOException exception if error while writing the output file
     */
    private void addAllCombinationsForPathAndPrefix(FPNode node, int[] prefix) throws IOException {
        // Concatenate the node item to the current prefix
        int[] itemset = new int[prefix.length + 1];
        System.arraycopy(prefix, 0, itemset, 0, prefix.length);
        itemset[prefix.length] = node.itemID;

        // save the resulting itemset to the file with its support
        saveItemset(itemset, node.counter);

        // recursive call if there is a node link
//		if(node.nodeLink != null){
//			addAllCombinationsForPathAndPrefix(node.nodeLink, prefix);
        if (node.childs.size() != 0) {
            addAllCombinationsForPathAndPrefix(node.childs.get(0), itemset);
            addAllCombinationsForPathAndPrefix(node.childs.get(0), prefix);
        }
    }

    /**
     * Write a frequent itemset that is found to the output file or keep into memory if the user prefer that the result be saved into memory.
     */
    private void saveItemset(int[] itemset, int support) {
        // increase the number of itemsets found for statistics purpose
        itemsetCount++;

        // We sort the itemset before showing it to the user so that it is
        // in lexical order.
        Arrays.sort(itemset);

        // create an object Itemset and add it to the set of patterns 
        // found.
        Itemset itemsetObj = new Itemset(itemset);

        itemsetObj.setAbsoluteSupport(support);
        patterns.addItemset(itemsetObj, itemsetObj.size());

    }

    /**
     * Print statistics about the algorithm execution to System.out.
     *
     * @return
     */
    public String printStats() {
        String stats = "";
        long temps = endTime - startTimestamp;
        stats += " Transactions found : " + transactionCount + "\n";
        stats += " Max memory usage: " + memoryLogger.getMaxMemory() + " mb \n";
        stats += " Frequent itemsets count : " + itemsetCount + "\n";
        stats += " Total time ~ " + temps + " ms";
        return stats;

    }

    /**
     * Get the number of transactions in the last transaction database read.
     *
     * @return the number of transactions.
     */
    public int getDatabaseSize() {
        return transactionCount;
    }

    public void reset() {
        patterns = null;
        transactionCount = 0;
        itemsetCount = 0;
        memoryLogger = null;
    }
}
