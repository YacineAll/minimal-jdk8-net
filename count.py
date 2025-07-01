db.collection.find({
  $expr: { $gt: [{ $size: "$listFieldName" }, 10] }
})