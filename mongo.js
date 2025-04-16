// MongoDB query to find all common IDs between documents that should be consolidated
db.businessObjects.aggregate([
  // Project relevant fields
  {
    $project: {
      _id: 1,
      type: 1,
      product: 1,
      subProduct: 1,
      ids: 1
    }
  },
  
  // Unwind the ids array
  {
    $unwind: "$ids"
  },
  
  // Group by ID to find documents sharing each ID
  {
    $group: {
      _id: "$ids",  // Group by individual ID
      documents: { 
        $addToSet: {
          docId: "$_id",
          type: "$type",
          product: "$product",
          subProduct: "$subProduct" 
        }
      },
      docCount: { $sum: 1 }
    }
  },
  
  // Filter for IDs that appear in multiple documents
  {
    $match: {
      "docCount": { $gt: 1 }
    }
  },
  
  // Group by the set of document IDs to consolidate all common IDs
  {
    $group: {
      _id: {
        // Create a sorted string of document IDs to group by
        documentSet: {
          $reduce: {
            input: "$documents.docId",
            initialValue: "",
            in: { 
              $concat: [
                "$$value", 
                { $cond: [{ $eq: ["$$value", ""] }, "", "-"] },
                { $toString: "$$this" }
              ]
            }
          }
        }
      },
      documentDetails: { $first: "$documents" },
      commonIds: { $addToSet: "$_id" },  // This collects all common IDs
      documentCount: { $first: "$docCount" }
    }
  },
  
  // Final projection for readability
  {
    $project: {
      _id: 0,
      documents: {
        $sortArray: {
          input: "$documentDetails",
          sortBy: { docId: 1 }
        }
      },
      commonIds: 1,
      commonIdCount: { $size: "$commonIds" }
    }
  },
  
  // Sort by document count
  {
    $sort: {
      "commonIdCount": -1
    }
  }
]);
