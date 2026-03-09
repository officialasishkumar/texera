# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

import builtins
import inspect
from contextlib import redirect_stdout
from io import StringIO
from typing import ContextManager

from core.util.buffer.buffer_base import IBuffer
from core.util.console_message.timestamp import current_time_in_local_timezone
from proto.org.apache.texera.amber.engine.architecture.rpc import (
    ConsoleMessage,
    ConsoleMessageType,
)


class replace_print(ContextManager):
    """
    A context manager to support replace builtin print function.

    With in the context, we use a customized print function which does the following:
    1. writes to a given buffer instead of stdout
    2. writes as a complete string, which is made of joining of all stringify-ed
    arguments and the end argument of the original print function. It calls the
    buf.write once per print call, which is different from
    contextlib.redirect_stdout who calls the buf.write for each argument in the
    print function.
    """

    def __init__(self, worker_id: str, buf: IBuffer):
        # save a reference to the original builtin.print before we replace it.
        # it will always replace back when the context manager exits, with exception
        # or not.
        self.builtins_print = builtins.print
        self.worker_id = worker_id
        self.buf = buf  # the provided buffer to write to

    def __enter__(self) -> None:
        """
        Enters the context, replace builtin.print function with a wrapped function.
        Now we hard code the wrapped_print to output complete print result to the
        given buffer.
        :return:
        """

        pass

    def __exit__(self, exc_type, exc_val, exc_tb) -> bool:
        """
        Exits the context, revert the replacement to recover the original
        builtin.print function.

        It does not handle exception within the context, it simply raises it outside
        the context.

        :param exc_type: potential exception type.
        :param exc_val: potential exception value.
        :param exc_tb: potential exception traceback.
        :return: bool, if no exception was raised, return True, otherwise,
        return False.
        """
        builtins.print = self.builtins_print
        return exc_val is None
