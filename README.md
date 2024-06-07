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
3) In a view, each message has a unique logical clock(which is message ID). Received messages are ordered by first scalar clock and then Ip address.
4) Receivers broadcast ACKs to response a certain message.
5) If a host receives all ACKs of a message from all other hosts; And that message is ranked first in the queue; And no ACKs to other messages indicating there is a message prior but not received. The host delivers the message.

### Characters

#### FIFO Queue
In our algorithm we assume that the channel is FIFO but actually it's not the case.
To solve this, all ACK and MSG messages have a sequence number initially set to zero.
FIFO Queue receives messages from MSGReveicer and it contains two maps. One is ExpectedSequenceNumber whose keys are Ip address followed by correct sequence of next message; The other map is FIFO Queue whose keys are Ip address followed by a queue containing messages whose sequence number is not expected.
After it receives message with correct sequence number, it checks whether messages in the queue can be released and deliver all messages that is FIFO to the ACK Manager or Message Buffer based on its type.

#### ACK Manager
ACK Manager receives messages from FIFO Queue and it is responsible for handling ACK messages and to check if a message is fully ACKed.
It has a map where the key is message ID(logical clock) followed by Ip addresses of hosts who have ACKed the message.
When it receives a new message it initializes its queue. When ACKs received it adds ACK's sender Ip to the queue.

ACK Manager also has a list called "WaitingForIt". When receiving ACKs of a message but that message is not received yet, it adds that message ID to the list and deletes it when the message comes.

#### Message Buffer
The Message Buffer receives message from FIFO Queue and is responsible for handling the message queue and delivery. It has a messageQueue which is a PriorityQueue, comparing first scalar clock and then Ip address. So that we can be sure that every member maintains the same order in the queue.
When receiving an ACK, the buffer tries to deliver. It asks the ACK Manager if the message on top is fully ACKed and pop it out if the answer is yes. However, it will also check WaitingForIt to make sure that there are no messages not received yet. It only deliver when WaitingForIt is empty to make sure that there is no mistake.
Eventually all buffers will have the same order and deliver it to the applications.

## 稿子
1. 发送基于UDP， 使用selector来制作receiver，全部的消息都使用broadcast，广播地址255.255.255.255 接收端口5000.
2. 使用2个thread，Msgreceiver负责接收，Alivesender负责发送alive，msgsender作为shared resrouce使用synchnized保证lock
3. 6种类型的msg type: msg的type
   type of msg
   ACK 收到消息发送ACK
   ALIVE 告诉主机自己的最后存活时间
   MSG 发送消息用
   DROP drop someone out if lastalive greater 5000*3 ago
   JOIN used for join the group
4. msg的组成
5. type: msg的type
   from: who is sender
   timestamp: send time
   view: view
   body: msg内容
   sequencenumber: 排序用
   scalarclock：排序用
6. 每次成员变动只能新增一名or移除一名，移除的优先级高于加入的优先级，加入的优先级取决于发送的时间。每一个成员只能发送一个end，除非他收到了另一个更高优先级的end。END的body里包含了全部的新成员
7. JOIN的新成员在from的field里，DROP要移除的成员在body里
8. 如果没连续收到2个ALIVE 就发送消息广播这个被DROP的成员
9. Recerver有4个state NEW（刚创建）-JOINING（尝试加入中）-JOINED（正常工作中）-CHANGE（变更成员中）