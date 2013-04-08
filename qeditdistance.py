########## Brianna Pritchett  ##########
########## bpritche           ##########

# This program is a bit of a test run for question edit distance - it is meant
# to take two files, one with questions (which will be given to us) and another
# one with questions and answers (which Dan and Maxime's program will produce)

import os, sys
import re
import string

"""
input = sys.argv
#The questions given, parsed
qstring = input[1]
#The possible questions, as a plaintext file
pqstring = input[2]
#'Possibles', so this is the list of all possible questions, parsed
pstring = input[3]
#'Possible answers', so this corresponds to the list of an answer for each question
# given in pfilename
astring = input[4]
#outputfilename = input[4]
"""

"""
pfile = open(pfilename)
qfile = open(qfilename)
afile = open(afilename)
pqfile = open(pqfilename)
"""

def strToList(s):
    #if(s[0] != '['): print s
    assert(len(s) > 0 and s[0] == '[')
    L = []
    elem = ''
    for char in s[1:]:
        if(char == ','):
            L.append(elem)
            elem = ''
        elif(char == ' '):
            continue
        elif(char == ']'):
            L.append(elem)
            break
        else:
            elem = elem + char
    return L

def savequestions(questionstring):
    questions = []
    postags = []
    dependencies = []
    sstags = []
    #The state can be 'pos', 'dep', 'sst', or 'blank'
    state = 'plaintext'
    for line in questionstring.splitlines():
        #line = line.replace('\n', '')
        
        if(state == 'plaintext'):
            questions.append(line)
            state = 'pos'        
        elif(state == 'pos'):
            #The questions given will already be POS tagged.  Here I'll just separate each word 
            # (which will look like, for instance, She/PRP) 
            # into a tuple (which will look like ('She', 'PRP')) to 
            # store in the list.
            # questions.append(line)
            #qtags = [nltk.tag.str2tuple(t) for t in line.split()]
            #Yikes, I just realized this is now a 2D list with tuples as elements - 
            # not space efficient.  Maybe I'll change it later?
            postags.append(strToList(line))
            state = 'dep'
        elif(state == 'dep'):
            dependencies.append(strToList(line))
            state = 'sst'
        elif(state == 'sst'):
            sstags.append(strToList(line))
            state = 'blank'
        elif(state == 'blank'):
            state = 'plaintext'
    
    return (questions, postags, dependencies, sstags)

"""
(ppostags, pdependencies, psstags) = savequestions(pstring)
assert(len(ppostags) == len(pdependencies))
assert(len(pdependencies) == len(psstags))
#pfile.close()

(qpostags, qdependencies, qsstags) = savequestions(qstring)
assert(len(qpostags) == len(qdependencies))
assert(len(qdependencies) == len(qsstags))
#qfile.close()
"""

def saveanswers(answerstring):
    answers = []
    for line in answerstring.splitlines():
        line = line.replace('\n', '')
        answers.append(line)
    return answers
    
"""
questions = saveanswers(pqstring)
assert(len(questions) == len(ppostags))
#pqfile.close()
alist = saveanswers(astring)
assert(len(alist) == len(ppostags))
#afile.close()
"""

'''
def min_edit_distance(q1, q2):
    #HERE WE GO
    #Let's say that q1 and q2 are lists of tuples like ('fly', NN), so that we can
    #readily access both the word and POS tags.
    #q1 will be our target - the question we are trying to find an answer to.
    #q2 will be the source - the question we are considering.
    n = len(q1)
    m = len(q2)

    distance = []
    #Initialize the 2D list to all 0's.
    for i in xrange(0, n + 1):
        distance.append([])
        for j in xrange(0, m + 1):
            distance[i].append(0)
    
    #Here is where I'll put all of the insertion and deletion and substitution
    #costs for the different parts of speech.  See edit-rules.txt.
    
    for i in xrange(1, n + 1):
    	#This will give the POS tag for the corresponding word in the question.
    	pos_tag = q1[i - 1][1]
    	distance[i][0] = distance[i - 1][0] + ins_cost[pos_tag]
    	#Not sure what the best way to do this is, but this should give the insertion
    	#cost for a word of this tag.  This will initialize the condition where we 
    	# simply insert each letter into the empty string.
    for j in xrange(1, m + 1):
    	pos_tag = q2[j - 1][1]
    	distance[0][j] = distance[0][j - 1] + del_cost[pos_tag]
    for i in xrange(1, n + 1):
    	for j in xrange(1, m + 1):
    		tpos_tag = q1[i - 1][1]
    		spos_tag = q2[j - 1][1]
    		inscost = distance[i - 1][j] + ins_cost[tpos_tag]
    		delcost = distance[i][j - 1] + del_cost[spos_tag]
    		subcost = distance[i - 1][j - 1] + sub_cost[tpos_tag][spos_tag]
    		#other things to consider - transcost, changing the cost for subbing
    		# words that are synonyms vs. antonyms vs. not related to each other. 
    		#Also, for some it would be worth deciding whether hypernyms and hyponyms.
    return distance[n][m]
'''

def filter(index, qpostags, qdependencies, qsstags, ppostags, pdependencies, psstags):
    #index will represent the index of qpostags (or qdependencies or qsstags) that I'm
    # considering.
    # We only need that one argument because ppostags, pdependencies, and psstags are
    # global variables, so we can iterate over them without having to pass them in as 
    # arguments.
    
    #These will represent the list of pos tags, dependencies, and super sense tags 
    # (respectively) for the question I'm considering
    qpos = qpostags[index]
    qdep = qdependencies[index]
    qsst = qsstags[index]
    
    #This will be a list of the proper nouns, if any, in the question.  We
    # will do a regular expression to look for these in the questionlist
    # because if two questions aren't both about the same proper noun, it
    # would be difficult for them to be similar, since proper nouns don't
    # have synonyms.
    pnouns = []
    for i in xrange(0, len(qpos)):
        tagged = qpos[i]
        #The proper nouns are represented by the POS tags NNPS or NNP.
        if(re.match(r'(.*NNP)|(.*NNPS)', tagged) != None):
            pnouns.append(tagged)
    
    #pquestions will be a list of tuples.  The first will be the the index of the question, 
    # on.  I'm using indices because I have multiple lists, and the thing in common
    # between them is the index.  Thus, ppostags[i] will represent the question for alist[i]
    # (hopefully).  The second will the score I'm assigning it, based on the similarities 
    # between the questions.
    pquestions = []
            
    #Iterate over all possible questions
    for i in xrange(0, len(ppostags)):
        simscore = 0
        ppos = ppostags[i]
        pdep = pdependencies[i]
        psst = psstags[i]
        
        #I don't think this nested for loop is too big of a deal
        # because pnouns probably won't have more than a couple 
        # elements
        '''
        for tagpair in pnouns:
            if(tagpair not in ppos):
                pquestions.append((question, i))
        '''
        for sstag in qsst:
            if(sstag != 0 and sstag in psst):
                simscore += 1 
        if(simscore >= 1):
            pquestions.append((i, simscore))
    if(len(pquestions) == 0): pquestions = questionlist
    #print pquestions
    return pquestions

#outputfile = open(outputfilename, 'w+')
def processquestions(qstring, pstring, astring):    
    (pos_questions, ppostags, pdependencies, psstags) = savequestions(pstring)
    
    assert(len(pos_questions) == len(ppostags))
    assert(len(ppostags) == len(pdependencies))
    assert(len(pdependencies) == len(psstags))
    #pfile.close()

    (ask_questions, qpostags, qdependencies, qsstags) = savequestions(qstring)
    assert(len(qpostags) == len(qdependencies))
    assert(len(qdependencies) == len(qsstags))
    
    alist = saveanswers(astring)
    assert(len(alist) == len(ppostags))

    for i in xrange(0, len(qpostags)):
        pquestions = filter(i, qpostags, qdependencies, qsstags, ppostags, pdependencies, psstags)
        #print pquestions
        if(len(pquestions) == 1): answer = alist[0][i]
        elif(len(pquestions) == 0): 
            #outputfile.write("This is a problem\n")
            answer = alist[0]
        #min_distance = 1000
        else:
            high_sim = 0
            closest_index = 0
            #cq_index = 0
            for i in xrange(0, len(pquestions)):
                pquestion = pquestions[i]
                sim = pquestion[1]
                #edit_distance = min_edit_distance(line, pquestion)
                '''
                if(edit_distance == 0):
                    closest_question = pquestion
                    min_distance = 0
                    break
                if(edit_distance < min_distance):
                    min_distance = edit_distance
                    closest_question = pquestion
                    cq_index = i
                '''
                if(sim > high_sim):
                    closest_index = pquestion[0]
                    high_sim = sim
                answer = alist[closest_index]
            #outputfile.write(answer + '\n')
        print "Question is \"%s\" and answer is \"%s\"\n" % (pos_questions[closest_index], answer)

#outputfile.close()