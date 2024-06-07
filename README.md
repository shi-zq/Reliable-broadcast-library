# slides

## general part
名字？
负责的部分？

## architecture part
得画一个架构图

UDP based

send-receive using broadcast on 255.255.255.255 port 5000

## sender-receiver

6 types of msg
type of msg
ACK 收到消息发送ACK
ALIVE 告诉主机自己的最后存活时间
MSG 发送消息用
DROP drop someone out if lastalive greater 5000*3 ago
JOIN used for join the group
END 提示大家自己已经完成了谁加入or谁踢出

要描写ReliableMsg吗？（我感觉要）

## ReliableMsg

type: msg的type
from: who is sender
timestamp: send time
view: view
body: msg内容
sequencenumber: 排序用
scalarclock：排序用

## join, drop end part

drop优先级比join高

end is a decision of who join or who leave, i cannt send 2 different end if didn't receive end from another client
i can change my end only if i receive a end from another client
only 1 change of member can happen join or drop(drop优先级比join高)

(end join)->receive drop-> shadowremoveip, i cannt send another end since it could create problem with double end
(end join)->receive end(drop)->change my decision from join to end
(end drop)->end(join)-> ignore
(end drop)->drop or join is irrelevant

## 排序部分

### Algorithm Description

Using logical clocks to achieve totally ordered multicase:
1) We suppose that communication channels are reliable and FIFO. 
2) logical clocks are composed from a scalar clock and ip address of a certain host.
3) In a view, each message has a unique logical clock(which is message ID). Received messages are ordered by first scalar clock and then IP.
4) Receivers broadcast ACKs to response a certain message.
5) If a host receives all ACKs of a message from all other hosts; And that message is ranked first in the queue; And no ACKs to other messages indicating there is a message prior but not received. The host delivers the message.

### Characters
#### ACK Manager
dsda
