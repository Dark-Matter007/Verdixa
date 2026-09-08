# Reference implementations for Verdixa's FUNCTION catalog. Function names follow its Python wrappers.
def missing_beacon(nums):
    answer = len(nums)
    for i, value in enumerate(nums): answer ^= i ^ value
    return answer

def letter_inventory_match(s, t):
    from collections import Counter
    return Counter(s) == Counter(t)

def stable_zero_shift(nums):
    write = 0
    for value in nums:
        if value:
            nums[write] = value
            write += 1
    for i in range(write, len(nums)): nums[i] = 0
    return nums

def bracket_balance(s):
    pairs, stack = {')':'(', ']':'[', '}':'{'}, []
    for ch in s:
        if ch in '([{': stack.append(ch)
        elif not stack or stack.pop() != pairs[ch]: return False
    return not stack

def longest_run_of_ones(nums):
    run = best = 0
    for value in nums:
        run = run + 1 if value == 1 else 0
        best = max(best, run)
    return best

def sorted_target_locator(nums, target):
    lo, hi = 0, len(nums)-1
    while lo <= hi:
        mid = (lo+hi)//2
        if nums[mid] == target: return mid
        if nums[mid] < target: lo = mid+1
        else: hi = mid-1
    return -1

def unique_window_length(s):
    last, left, best = {}, 0, 0
    for right, ch in enumerate(s):
        left = max(left, last.get(ch, -1)+1)
        last[ch] = right
        best = max(best, right-left+1)
    return best

def product_without_position(nums):
    result, product = [1]*len(nums), 1
    for i, value in enumerate(nums):
        result[i] = product
        product *= value
    product = 1
    for i in range(len(nums)-1, -1, -1):
        result[i] *= product
        product *= nums[i]
    return result

def frequency_crown(nums, k):
    from collections import Counter
    counts = Counter(nums)
    return sorted(counts, key=lambda value: (-counts[value], value))[:k]

def quiet_street_robber(nums):
    older = previous = 0
    for value in nums: older, previous = previous, max(previous, older+value)
    return previous

def well_formed_parenthesis_builder(n):
    result = []
    def visit(text, opened, closed):
        if closed == n:
            result.append(text)
            return
        if opened < n: visit(text+'(', opened+1, closed)
        if closed < opened: visit(text+')', opened, closed+1)
    visit('', 0, 0)
    return result

def prerequisite_route_check(courseCount, prerequisites):
    from collections import deque
    graph, degree = [[] for _ in range(courseCount)], [0]*courseCount
    for edge in prerequisites:
        course, pre = map(int, edge.split(','))
        graph[pre].append(course)
        degree[course] += 1
    queue = deque(i for i in range(courseCount) if degree[i] == 0)
    count = 0
    while queue:
        node = queue.popleft()
        count += 1
        for nxt in graph[node]:
            degree[nxt] -= 1
            if degree[nxt] == 0: queue.append(nxt)
    return count == courseCount

def daily_temperature_wait(temperatures):
    result, stack = [0]*len(temperatures), []
    for i, value in enumerate(temperatures):
        while stack and temperatures[stack[-1]] < value:
            j = stack.pop()
            result[j] = i-j
        stack.append(i)
    return result

def circular_fuel_departure(fuel, cost):
    if not fuel: return -1
    total = tank = start = 0
    for i in range(len(fuel)):
        delta = fuel[i]-cost[i]
        total += delta
        tank += delta
        if tank < 0: start, tank = i+1, 0
    return start if total >= 0 else -1

def rainwater_basin(heights):
    lo, hi, left, right, water = 0, len(heights)-1, 0, 0, 0
    while lo <= hi:
        if left <= right:
            left = max(left, heights[lo])
            water += left-heights[lo]
            lo += 1
        else:
            right = max(right, heights[hi])
            water += right-heights[hi]
            hi -= 1
    return water

def histogram_skyline(heights):
    stack, best = [], 0
    for i in range(len(heights)+1):
        current = heights[i] if i < len(heights) else 0
        while stack and heights[stack[-1]] > current:
            height = heights[stack.pop()]
            width = i - (stack[-1] if stack else -1) - 1
            best = max(best, height*width)
        stack.append(i)
    return best

def string_transformation_cost(source, target):
    previous = list(range(len(target)+1))
    for i, a in enumerate(source, 1):
        current = [i]
        for j, b in enumerate(target, 1):
            current.append(previous[j-1] if a == b else 1+min(previous[j-1], previous[j], current[-1]))
        previous = current
    return previous[-1]

def smallest_covering_window(source, target):
    from collections import Counter
    if not target: return ''
    need, missing, left, start, length = Counter(target), len(target), 0, 0, len(source)+1
    for right, ch in enumerate(source):
        if need[ch] > 0: missing -= 1
        need[ch] -= 1
        while missing == 0:
            if right-left+1 < length: start, length = left, right-left+1
            need[source[left]] += 1
            if need[source[left]] > 0: missing += 1
            left += 1
    return '' if length > len(source) else source[start:start+length]

def word_mutation_steps(beginWord, endWord, dictionary):
    from collections import deque
    if beginWord == endWord: return 1
    remaining = set(dictionary)
    if endWord not in remaining: return 0
    remaining.discard(beginWord)
    queue = deque([(beginWord, 1)])
    while queue:
        word, distance = queue.popleft()
        for i in range(len(word)):
            for ch in 'abcdefghijklmnopqrstuvwxyz':
                nxt = word[:i]+ch+word[i+1:]
                if nxt not in remaining: continue
                if nxt == endWord: return distance+1
                remaining.remove(nxt)
                queue.append((nxt, distance+1))
    return 0

def queen_board_counter(n):
    mask = (1 << n)-1
    def visit(columns, left, right):
        if columns == mask: return 1
        free, result = mask & ~(columns | left | right), 0
        while free:
            bit = free & -free
            free -= bit
            result += visit(columns | bit, ((left | bit) << 1) & mask, (right | bit) >> 1)
        return result
    return visit(0, 0, 0)

def majority_signal(nums):
    candidate = balance = 0
    for value in nums:
        if balance == 0: candidate = value
        balance += 1 if value == candidate else -1
    return candidate

def prefix_balance_point(nums):
    total, left = sum(nums), 0
    for i, value in enumerate(nums):
        if left == total-left-value: return i
        left += value
    return -1

def single_number_trace(nums):
    answer = 0
    for value in nums: answer ^= value
    return answer

def first_unique_symbol(s):
    from collections import Counter
    counts = Counter(s)
    return next((i for i, ch in enumerate(s) if counts[ch] == 1), -1)

def rotated_array_locator(nums, target):
    lo, hi = 0, len(nums)-1
    while lo <= hi:
        mid = (lo+hi)//2
        if nums[mid] == target: return mid
        if nums[lo] <= nums[mid]:
            if nums[lo] <= target < nums[mid]: hi = mid-1
            else: lo = mid+1
        else:
            if nums[mid] < target <= nums[hi]: lo = mid+1
            else: hi = mid-1
    return -1

def peak_terrain_index(heights):
    lo, hi = 0, len(heights)-1
    while lo < hi:
        mid = (lo+hi)//2
        if heights[mid] < heights[mid+1]: lo = mid+1
        else: hi = mid
    return lo

def minimum_capacity_scheduler(weights, days):
    lo, hi = max(weights), sum(weights)
    while lo < hi:
        capacity, used, load = (lo+hi)//2, 1, 0
        for weight in weights:
            if load+weight > capacity: used, load = used+1, 0
            load += weight
        if used <= days: hi = capacity
        else: lo = capacity+1
    return lo

def kth_largest_stream_value(nums, k):
    import heapq
    heap = []
    for value in nums:
        heapq.heappush(heap, value)
        if len(heap) > k: heapq.heappop(heap)
    return heap[0]

def merge_time_blocks(blocks):
    merged = []
    for start, end in sorted(tuple(map(int, block.split(','))) for block in blocks):
        if merged and start <= merged[-1][1]: merged[-1][1] = max(merged[-1][1], end)
        else: merged.append([start, end])
    return [str(start)+','+str(end) for start, end in merged]

def minimum_meeting_rooms(meetings):
    import heapq
    heap, best = [], 0
    for start, end in sorted(tuple(map(int, item.split(','))) for item in meetings):
        while heap and heap[0] <= start: heapq.heappop(heap)
        heapq.heappush(heap, end)
        best = max(best, len(heap))
    return best

def next_greater_circular_value(nums):
    result, stack, n = [-1]*len(nums), [], len(nums)
    for i in range(2*n):
        while stack and nums[stack[-1]] < nums[i % n]: result[stack.pop()] = nums[i % n]
        if i < n: stack.append(i)
    return result

def sliding_window_maximum(nums, k):
    from collections import deque
    queue, result = deque(), []
    for i, value in enumerate(nums):
        while queue and queue[0] <= i-k: queue.popleft()
        while queue and nums[queue[-1]] <= value: queue.pop()
        queue.append(i)
        if i >= k-1: result.append(nums[queue[0]])
    return result

def subarray_target_count(nums, target):
    counts, prefix, result = {0:1}, 0, 0
    for value in nums:
        prefix += value
        result += counts.get(prefix-target, 0)
        counts[prefix] = counts.get(prefix, 0)+1
    return result

def longest_consecutive_chain(nums):
    values, best = set(nums), 0
    for value in values:
        if value-1 not in values:
            end = value
            while end in values: end += 1
            best = max(best, end-value)
    return best

def coin_combination_minimum(coins, amount):
    dp = [0]+[amount+1]*amount
    for value in range(1, amount+1):
        for coin in coins:
            if coin <= value: dp[value] = min(dp[value], dp[value-coin]+1)
    return dp[amount] if dp[amount] <= amount else -1

def increasing_sequence_length(nums):
    from bisect import bisect_left
    tails = []
    for value in nums:
        i = bisect_left(tails, value)
        if i == len(tails): tails.append(value)
        else: tails[i] = value
    return len(tails)

def partition_target_feasibility(nums):
    total = sum(nums)
    if total % 2: return False
    target, possible = total//2, [True]+[False]*(total//2)
    for value in nums:
        for amount in range(target, value-1, -1): possible[amount] |= possible[amount-value]
    return possible[target]

def decode_message_ways(digits):
    older, previous = 1, int(digits[0] != '0')
    for i in range(1, len(digits)):
        current = previous if digits[i] != '0' else 0
        if 10 <= int(digits[i-1:i+1]) <= 26: current += older
        older, previous = previous, current
    return previous

def matrix_spiral_reader(rows):
    if not rows or not rows[0]: return ''
    top, bottom, left, right, result = 0, len(rows)-1, 0, len(rows[0])-1, []
    while top <= bottom and left <= right:
        for j in range(left, right+1): result.append(rows[top][j])
        top += 1
        for i in range(top, bottom+1): result.append(rows[i][right])
        right -= 1
        if top <= bottom:
            for j in range(right, left-1, -1): result.append(rows[bottom][j])
            bottom -= 1
        if left <= right:
            for i in range(bottom, top-1, -1): result.append(rows[i][left])
            left += 1
    return ''.join(result)

def connected_region_counter(rows):
    if not rows or not rows[0]: return 0
    grid, count = [list(row) for row in rows], 0
    for i in range(len(grid)):
        for j in range(len(grid[0])):
            if grid[i][j] != '1': continue
            count += 1
            stack, grid[i][j] = [(i,j)], '0'
            while stack:
                x,y = stack.pop()
                for a,b in ((x+1,y),(x-1,y),(x,y+1),(x,y-1)):
                    if 0 <= a < len(grid) and 0 <= b < len(grid[0]) and grid[a][b] == '1':
                        grid[a][b] = '0'
                        stack.append((a,b))
    return count

def median_of_two_sorted_sequences(a, b):
    if len(a) > len(b): a,b = b,a
    lo, hi, half = 0, len(a), (len(a)+len(b)+1)//2
    while lo <= hi:
        i = (lo+hi)//2
        j = half-i
        al, ar = a[i-1] if i else float('-inf'), a[i] if i < len(a) else float('inf')
        bl, br = b[j-1] if j else float('-inf'), b[j] if j < len(b) else float('inf')
        if al <= br and bl <= ar:
            return float(max(al,bl)) if (len(a)+len(b))%2 else (max(al,bl)+min(ar,br))/2
        if al > br: hi = i-1
        else: lo = i+1

def maximum_rectangle_in_binary_grid(rows):
    if not rows: return 0
    heights, best = [0]*len(rows[0]), 0
    for row in rows:
        heights = [height+1 if ch == '1' else 0 for height,ch in zip(heights,row)]
        stack = []
        for i in range(len(heights)+1):
            current = heights[i] if i < len(heights) else 0
            while stack and heights[stack[-1]] > current:
                h = heights[stack.pop()]
                best = max(best,h*(i-(stack[-1] if stack else -1)-1))
            stack.append(i)
    return best

def longest_valid_bracket_segment(s):
    stack, best = [-1], 0
    for i,ch in enumerate(s):
        if ch == '(': stack.append(i)
        else:
            stack.pop()
            if not stack: stack.append(i)
            else: best = max(best,i-stack[-1])
    return best

def burst_value_optimizer(nums):
    values = [1]+nums+[1]
    n = len(values)
    dp = [[0]*n for _ in range(n)]
    for width in range(2,n):
        for left in range(n-width):
            right = left+width
            dp[left][right] = max(dp[left][k]+dp[k][right]+values[left]*values[k]*values[right] for k in range(left+1,right))
    return dp[0][-1]

def palindrome_minimum_cuts(s):
    n = len(s)
    if n == 0: return 0
    pal, cuts = [[False]*n for _ in range(n)], list(range(-1,n))
    for right in range(n):
        for left in range(right+1):
            if s[left] == s[right] and (right-left < 2 or pal[left+1][right-1]):
                pal[left][right] = True
                cuts[right+1] = min(cuts[right+1],cuts[left]+1)
    return cuts[n]

def network_delay_minimum(edges, nodeCount, source):
    import heapq
    graph = [[] for _ in range(nodeCount+1)]
    for edge in edges:
        a,b,w = map(int,edge.split(','))
        graph[a].append((b,w))
    distance, heap = [float('inf')]*(nodeCount+1), [(0,source)]
    distance[source] = 0
    while heap:
        cost,node = heapq.heappop(heap)
        if cost != distance[node]: continue
        for nxt,w in graph[node]:
            if cost+w < distance[nxt]:
                distance[nxt] = cost+w
                heapq.heappush(heap,(cost+w,nxt))
    result = max(distance[1:])
    return -1 if result == float('inf') else result

def cheapest_route_with_stop_limit(edges, cityCount, source, destination, maxStops):
    flights = [tuple(map(int,edge.split(','))) for edge in edges]
    distance = [float('inf')]*cityCount
    distance[source] = 0
    for _ in range(maxStops+1):
        nxt = distance.copy()
        for a,b,cost in flights: nxt[b] = min(nxt[b],distance[a]+cost)
        distance = nxt
    return -1 if distance[destination] == float('inf') else distance[destination]

def maximum_job_profit_schedule(start, end, profit):
    from bisect import bisect_right
    jobs = sorted(zip(end,start,profit))
    finishes, dp = [], [0]
    for finish,begin,value in jobs:
        previous = bisect_right(finishes,begin)
        dp.append(max(dp[-1],dp[previous]+value))
        finishes.append(finish)
    return dp[-1]

def regex_pattern_matcher(text, pattern):
    n,m = len(text),len(pattern)
    dp = [[False]*(m+1) for _ in range(n+1)]
    dp[0][0] = True
    for j in range(2,m+1):
        if pattern[j-1] == '*': dp[0][j] = dp[0][j-2]
    for i in range(1,n+1):
        for j in range(1,m+1):
            if pattern[j-1] == '*':
                dp[i][j] = dp[i][j-2] or (pattern[j-2] in ('.',text[i-1]) and dp[i-1][j])
            else: dp[i][j] = pattern[j-1] in ('.',text[i-1]) and dp[i-1][j-1]
    return dp[n][m]

def course_completion_ordering(courseCount, prerequisites):
    import heapq
    graph,degree = [[] for _ in range(courseCount)],[0]*courseCount
    for edge in prerequisites:
        course,pre = map(int,edge.split(','))
        graph[pre].append(course)
        degree[course] += 1
    heap = [i for i in range(courseCount) if degree[i] == 0]
    heapq.heapify(heap)
    order = []
    while heap:
        node = heapq.heappop(heap)
        order.append(node)
        for nxt in graph[node]:
            degree[nxt] -= 1
            if degree[nxt] == 0: heapq.heappush(heap,nxt)
    return order if len(order) == courseCount else []

def reverse_string(s):
    return s[::-1]

def two_sum(nums, target):
    seen = {}
    for i,value in enumerate(nums):
        if target-value in seen: return [seen[target-value],i]
        seen[value] = i
    return []

def is_valid(s):
    pairs, stack = {')':'(', ']':'[', '}':'{'}, []
    for ch in s:
        if ch in '([{': stack.append(ch)
        elif not stack or stack.pop() != pairs[ch]: return False
    return not stack
