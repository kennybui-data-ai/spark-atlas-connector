import argparse
import json
import os

from pyapacheatlas.auth import ServicePrincipalAuthentication
from pyapacheatlas.core import PurviewClient  # Communicate with your Atlas server
from pyapacheatlas.core import EntityTypeDef, RelationshipTypeDef

import logging
from pprint import pformat


def formatlog(func):
    """decorator to format log message
    """
    def wrapper(*args):
        if len(args) == 1:
            msg = args[0]
            return func(
                pformat(msg) if not isinstance(msg, str) else msg
            )
        elif len(args) == 2:
            _, msg = args
            return func(
                _,
                pformat(msg) if not isinstance(msg, str) else msg
            )
        else:
            raise ValueError(
                f"invalid number of args for formatlog: len(args) == {len(args)}")

    return wrapper


class CustomLogger:
    def __init__(self, logger):
        self.logger = logger

    @formatlog
    def info(self, msg):
        """info log

        :param msg: log message
        :type msg: str or any
        """
        self.logger.info(msg)

    @formatlog
    def warning(self, msg):
        """warning log

        :param msg: log message
        :type msg: str or any
        """
        self.logger.warning(msg)

    @formatlog
    def error(self, msg):
        """error log

        :param msg: log message
        :type msg: str or any
        """
        self.logger.error(msg)

    @formatlog
    def critical(self, msg):
        """critical log

        :param msg: log message
        :type msg: str or any
        """
        self.logger.critical(msg)

    @formatlog
    def debug(self, msg):
        """debug log

        :param msg: log message
        :type msg: str or any
        """
        self.logger.debug(msg)


def parse_cmdline():
    parser = argparse.ArgumentParser(
        description="Create spark model in Azure Purview"
    )
    parser.add_argument("-t", "--tenant_id", type=str, required=True,
                        help="AAD Tenant ID")
    parser.add_argument("-c", "--client_id", type=str, required=True,
                        help="SPN Client/Application ID")
    parser.add_argument("-s", "--client_secret", type=str, required=True,
                        help="SPN Secret")

    default_purview = "yaaf-purview"
    parser.add_argument("-n", "--purview_name", type=str, default=default_purview,
                        help=f"default is {default_purview}")

    default_spark_model = "1100-spark_model.json"
    parser.add_argument("--spark_model", type=str, default=default_spark_model,
                        help=f"default is {default_spark_model}")

    parser.add_argument("--debug", action='store_true',
                        help=f"specify flag to enable debug logging")

    return parser.parse_args()


if __name__ == "__main__":
    args = parse_cmdline()

    if args.debug:
        logging.basicConfig(level=logging.DEBUG)
    else:
        logging.basicConfig(level=logging.INFO)

    logger = CustomLogger(logging.getLogger(__file__))

    # Authenticate against your Atlas server
    oauth = ServicePrincipalAuthentication(
        tenant_id=args.tenant_id,
        client_id=args.client_id,
        client_secret=args.client_secret
    )
    client = PurviewClient(
        account_name=args.purview_name,
        authentication=oauth
    )

    with open(args.spark_model, "r") as f:
        spark_model = json.load(f)

    to_pop = ["enumDefs", "structDefs", "classificationDefs"]
    for p in to_pop:
        logger.debug(f"popping key: {p}")
        logger.debug(spark_model.pop(p))

    for k, v in spark_model.items():
        if k in to_pop:
            raise ValueError(f"failed to pop key {k}")
        else:
            logger.info(f"valid key: {k}")

    # entitydefs = spark_model["entityDefs"]
    # relationshipdefs = spark_model["relationshipDefs"]

    # for ent in entitydefs:
    #     # print(ent["name"])
    #     temp_entity = EntityTypeDef(**ent)
    #     logger.info(vars(temp_entity))

    # for rel in relationshipdefs:
    #     # print(rel["name"])
    #     temp_relationship = RelationshipTypeDef(**rel)
    #     logger.info(vars(temp_relationship))

    upload_results = client.upload_typedefs(spark_model)
    logger.info(upload_results)
