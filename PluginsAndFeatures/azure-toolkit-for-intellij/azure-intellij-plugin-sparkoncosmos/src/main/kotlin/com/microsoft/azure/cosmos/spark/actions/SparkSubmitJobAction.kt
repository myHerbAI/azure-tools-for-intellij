package com.microsoft.azure.cosmos.spark.actions

import com.microsoft.azure.hdinsight.spark.run.action.SeqActions

class CosmosServerlessSparkSelectAndSubmitAction : SeqActions("Actions.SelectCosmosServerlessSparkType", "Actions.SubmitSparkApplicationAction")

class CosmosSparkSelectAndSubmitAction : SeqActions("Actions.SelectCosmosSparkType", "Actions.SubmitSparkApplicationAction")